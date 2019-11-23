package com.bjpowernode.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSONObject;
import com.bjpowernode.common.Contants;
import com.bjpowernode.common.ReturnObject;
import com.bjpowernode.mapper.GoodsMapper;
import com.bjpowernode.model.Goods;
import com.bjpowernode.model.Orders;
import com.bjpowernode.service.GoodsService;
import com.sun.org.apache.xalan.internal.xsltc.compiler.Constants;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName GoodsServiceImpl
 * @Description: TODO
 * @Author codemi@aliyun.com
 * @Date 2019/11/21
 **/

@Service(interfaceClass = GoodsService.class)
@Component
public class GoodsServiceImpl implements GoodsService{

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private JmsTemplate jmsTemplate;
    private StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

    @Resource
    private GoodsMapper goodsMapper;

    @Override
    public List<Goods> selectAll() {
        return goodsMapper.selectAll();
    }

    @Override
    public Goods selectById(Integer id) {
        return goodsMapper.selectByPrimaryKey(id);
    }

    @Override
    public ReturnObject seckill(Integer goodsId, String randomName, Integer uid) {
        ReturnObject returnObject = ReturnObject.getInstance();
        redisTemplate.setKeySerializer(stringRedisSerializer);
        redisTemplate.setValueSerializer(stringRedisSerializer);
        String strStore = (String) redisTemplate.opsForValue().get(Contants.GOODS_STORE+randomName);
        //进入if表示当前商品随即名在redis中并不存在，可能是因为用户手动发送的请求，伪造参数
        if (strStore == null){
            returnObject.setCode(Contants.ERROR);
            returnObject.setMessage("您所秒杀的商品异常！");
            returnObject.setResult("");
            return returnObject;
        }
        //进入if表示当前商品库存不足
        if (Integer.valueOf(strStore) <= 0){
            returnObject.setCode(Contants.ERROR);
            returnObject.setMessage("对不起！您所购买的商品已被抢光！");
            returnObject.setResult("");
            return returnObject;
        }

        //根据固定的前缀+商品随机名+用户id判断当前用户是否购买过这个产品
        String purchaseLimits = (String) redisTemplate.opsForValue().get(Contants.PURCHASE_LIMITS+randomName+uid);
        if (purchaseLimits != null){
            returnObject.setCode(Contants.ERROR);
            returnObject.setMessage("对不起！您已经购买过这个商品了！");
            returnObject.setResult("");
            return returnObject;
        }

        //增加限流人数，人数+1，并返回本次增加的结果
        Long currentLimiting = redisTemplate.opsForValue().increment(Contants.CURRENT_LIMITING);
        //如果自增的返回值大于1000则进入if表示用户超出限流人数
        if (currentLimiting > 1000){
            returnObject.setCode(Contants.ERROR);
            returnObject.setMessage("对不起！服务器繁忙请稍后再试！");
            returnObject.setResult("");
            redisTemplate.opsForValue().decrement(Contants.CURRENT_LIMITING);
            return returnObject;
        }

        //创建订单对象，用户redis记录订单防止掉单和将订单存入队列用于通知订单系统完成数据库的下单操作
        Orders orders = new Orders();
        orders.setGoodsid(goodsId);
        orders.setUid(uid);
        //利用Redis事务解决超卖问题 execute执行成功后返回一个数据利用这个数据判断事务是否执行成功
        Object result = redisTemplate.execute(new SessionCallback() {

            public Object execute(RedisOperations redisOperations) throws DataAccessException {
                //定义需要监控的key的List集合
                List<String> list = new ArrayList<String>();
                list.add(Contants.GOODS_STORE+randomName); //监控商品库存防止超卖
                list.add(Contants.PURCHASE_LIMITS+randomName+uid); //监控限购的key防止用户重复提交
                redisOperations.watch(list);
                //获取商品库存
                Integer store = Integer.valueOf((String)redisOperations.opsForValue().get(Contants.GOODS_STORE+randomName));
                if (store <= 0){
                    returnObject.setCode(Contants.ERROR);
                    returnObject.setMessage("对不起！您所购买的商品已被抢光！");
                    returnObject.setResult("");
                    return returnObject;
                }
                //程序到了这里表示一定有库存，但是却不一定购买成功
                //进入if表示当前用户已经购买过商品了
                if(redisOperations.opsForValue().get(Contants.PURCHASE_LIMITS+randomName+uid)!=null){
                    returnObject.setCode(Contants.ERROR);
                    returnObject.setMessage("对不起！您已经购买过这个商品了！");
                    returnObject.setResult("");
                    return returnObject;
                }
                redisOperations.multi();
                redisOperations.opsForValue().decrement(Contants.GOODS_STORE+randomName);
                //添加一条限购记录，使用任意非null的数据作为value
                redisOperations.opsForValue().set(Contants.PURCHASE_LIMITS+randomName+uid,"1");
                //redis中记录订单消息，防止掉单
                //使用固定前缀+商品id+用户id作为key 使用订单对象的json数据作为value
                //然后可以开启定时任务，定期扫描redis判断哪些订单没有完成，如果没有完成及向消息队列中写入一条数据
                //当数据库下单成功后可以将这个数据从redis中移除
                redisOperations.opsForValue().set(Contants.ORDERS+goodsId+uid, JSONObject.toJSONString(orders));

                return redisOperations.exec();
            }
        });
        //进入if表示redis事务执行方法返回值为ReturnObject 表示在执行事务之前九已经返回了，可能没有库存了或者用户已经购买过了
        if (result instanceof ReturnObject){
            return (ReturnObject) result;
        }
        List list = (List) result;
        //进入if表示返回值result是List类型表示redis的事务已经提交了，但是没有提交成功
        //原因是可能有其他的线程修改了数据（具体原因不详）
        //这里不能直接判断是否是错误或其他异常，可能仅仅就是库存被修改了但是还有剩余，处理方式有2种
        //1、直接返回给用户一个提示，提示服务器繁忙（这种情况可能会造成大量用户返回页面）
        //2、递归调用自动进入下一轮抢购，重新执行业务逻辑，这样可能会导致请求时间过长
        if (list.isEmpty()){
            //递归减少限流人数，防止一个用户占用2个限流位置
            redisTemplate.opsForValue().decrement(Contants.CURRENT_LIMITING);
            return this.seckill(goodsId,randomName,uid);
        }

        //将消息对象的Json存入MQ中，通知订单系统来完成商品的下单操作
        jmsTemplate.send(new MessageCreator() {
            @Override
            public Message createMessage(Session session) throws JMSException {
                return session.createTextMessage(JSONObject.toJSONString(orders));
            }
        });

        //减少限流人数
        redisTemplate.opsForValue().decrement(Contants.CURRENT_LIMITING);
        returnObject.setCode(Contants.OK);
        returnObject.setMessage("下单成功");
        returnObject.setResult("");
        return returnObject;
    }
}
