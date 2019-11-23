package com.bjpowernode.service;

import com.bjpowernode.common.ReturnObject;
import com.bjpowernode.model.Goods;

import java.util.List;

/**
 * @ClassName GoodsService
 * @Description: TODO
 * @Author 876666981@qq.com
 * @Date 2019/11/21
 **/

public interface GoodsService {
    List<Goods> selectAll();

    Goods selectById(Integer id);


    ReturnObject seckill(Integer goodsId, String randomName, Integer uid);
}
