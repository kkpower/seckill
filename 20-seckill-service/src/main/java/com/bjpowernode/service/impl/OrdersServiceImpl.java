package com.bjpowernode.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSONObject;
import com.bjpowernode.common.Contants;
import com.bjpowernode.mapper.GoodsMapper;
import com.bjpowernode.mapper.OrdersMapper;
import com.bjpowernode.model.Goods;
import com.bjpowernode.model.Orders;
import com.bjpowernode.service.OrdersService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;

/**
 * @ClassName OrdersServiceImpl
 * @Description: TODO
 * @Author codemi@aliyun.com
 * @Date 2019/11/22
 **/

@Service(interfaceClass = OrdersService.class)
@Component
public class OrdersServiceImpl implements OrdersService{

    @Resource
    private OrdersMapper ordersMapper;
    @Resource
    private GoodsMapper goodsMapper;
    @Resource
    private RedisTemplate redisTemplate;
    private StringRedisSerializer stringRedisSerializer=new StringRedisSerializer();;

    @Override
    public int addSecKillOrder(Orders orders) {
        redisTemplate.setValueSerializer(stringRedisSerializer);
        redisTemplate.setKeySerializer(stringRedisSerializer);
        Goods goods= goodsMapper.selectByPrimaryKey(orders.getGoodsid());
        orders.setCreatetime(new Date());
        orders.setStatus(1); //订单状态1 表示未支付
        orders.setBuynum(1);
        orders.setBuyprice(goods.getPrice());
        orders.setOrdermoney(goods.getPrice().multiply(new BigDecimal(orders.getBuynum())));
        try {
            //这里可能会出现异常，当消息队列中有重复数据时这里就可能会出现异常，违反数据库的唯一约束
            //这种情况表示这条消息（订单）已经进入了数据库中我们应该跳过这个处理
            //第四次防止用户重复购买的操作
            ordersMapper.insert(orders);
            //使用固定的前缀+用户id+商品id 作为key 使用订单的记录作为value将下单成功的结果写入Redis中用于通知用户进行支付
            redisTemplate.opsForValue().set(Contants.ORDER_RESULT+orders.getUid()+orders.getGoodsid(), JSONObject.toJSONString(orders));
            redisTemplate.delete(Contants.ORDERS+orders.getGoodsid()+orders.getUid());
        } catch (Exception e) {
            e.printStackTrace();
            //进入catch可能是因为违反唯一约束表示消息已经处理过了应该从Redis中移除订单记录
            //基本上Redis中的订单早就被移除了，以防万一
            redisTemplate.delete(Contants.ORDERS+orders.getGoodsid()+orders.getUid());
        }
        return 0;
    }
}
