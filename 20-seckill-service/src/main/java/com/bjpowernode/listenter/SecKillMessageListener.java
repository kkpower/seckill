package com.bjpowernode.listenter;

import com.alibaba.fastjson.JSONObject;
import com.bjpowernode.model.Orders;
import com.bjpowernode.service.OrdersService;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @ClassName SecKillMessageListener
 * @Description: TODO
 * @Author codemi@aliyun.com
 * @Date 2019/11/22
 **/

@Component
public class SecKillMessageListener {
    @Resource
    private OrdersService ordersService;

    @JmsListener(destination = "seckill")
    public void onMessage(String message){
        //获取消息队列中的订单数据并转换成Orders对象
        Orders orders = JSONObject.parseObject(message, Orders.class);
        //调用订单业务方法完成数据库的下单
        int result = ordersService.addSecKillOrder(orders);
    }
}
