package com.bjpowernode.SecKillScheduled;

import com.bjpowernode.common.Contants;
import com.bjpowernode.mapper.GoodsMapper;
import com.bjpowernode.model.Goods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @ClassName scheduled
 * @Description: TODO
 * @Author codemi@aliyun.com
 * @Date 2019/11/22
 **/
@EnableScheduling
@Component
public class scheduled {

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    private StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

    //创建一个定时任务每5秒钟执行一次定时任务，用于将数据库中的数据写入到redis中
    //实际工作中不能是每5秒钟一次，在某个固定的时间执行例如秒杀开始前的几分钟或者每天的0点初始化今天的所有商品
    @Scheduled(cron = "0/5 * * * * *")
    public void initStoreToRedis(){
        redisTemplate.setKeySerializer(stringRedisSerializer);
        redisTemplate.setValueSerializer(stringRedisSerializer);
        //获取所有的商品
        //实际工作中不能获取所有的商品，应该根据时间获取即将开始秒杀的商品
        List<Goods> goodsList = goodsMapper.selectAll();
        for (Goods goods : goodsList) {
            //如果key操作则放弃数据插入
            //如果key不存在则使用固定的前缀+商品随即名作为key 商品库存作为value将数据写入redis中
            redisTemplate.opsForValue().setIfAbsent(Contants.GOODS_STORE+goods.getRandomname(),goods.getStore()+"");
        }
    }
}
