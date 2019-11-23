package com.bjpowernode.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.bjpowernode.common.Contants;
import com.bjpowernode.common.ReturnObject;
import com.bjpowernode.model.Goods;
import com.bjpowernode.service.GoodsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * @ClassName GoodController
 * @Description: TODO
 * @Author codemi@aliyun.com
 * @Date 2019/11/21
 **/

@Controller
public class GoodController {

    @Reference
    private GoodsService goodsService;

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String index(Model model){
        List<Goods> goodsList = goodsService.selectAll();
        model.addAttribute("goodsList",goodsList);
        return "goods/index";
    }

    @RequestMapping(value = "/showgoods/{id}", method = RequestMethod.GET)
    public String showGoods(Model model, @PathVariable Integer id){

        Goods goods = goodsService.selectById(id);
        model.addAttribute("goods",goods);
        return "goods/showgoods";
    }

    /**
     * 获取系统当前时间
     * @return
     */
    @RequestMapping(value = "/getSystemTime",method = RequestMethod.GET)
    @ResponseBody
    public Long getSystemTime(){
        return System.currentTimeMillis();
    }

    @RequestMapping(value = "/getRandomName/{id}",method = RequestMethod.GET)
    public @ResponseBody Object getRandomName(@PathVariable Integer id){

         Goods goods = goodsService.selectById(id);
         ReturnObject returnObject =  ReturnObject.getInstance();
         if (goods == null){

             returnObject.setCode(Contants.ERROR);
             returnObject.setMessage("商品消息异常!请确认!");
             returnObject.setResult("");
             return returnObject;
         }
         Long nowTime = System.currentTimeMillis();
         if (nowTime < goods.getStarttime().getTime()){
             returnObject.setCode(Contants.ERROR);
             returnObject.setMessage("秒杀活动还没有开始请稍后再试");
             returnObject.setResult("");
             return returnObject;
         }
        if (nowTime > goods.getEndtime().getTime()){
            returnObject.setCode(Contants.ERROR);
            returnObject.setMessage("秒杀活动已结束");
            returnObject.setResult("");
            return returnObject;
        }
        if (goods.getRandomname() == null || "".equals(goods.getRandomname())){
            returnObject.setCode(Contants.ERROR);
            returnObject.setMessage("商品消息异常!请确认!");
            returnObject.setResult("");
            return returnObject;
        }

        returnObject.setCode(Contants.OK);
        returnObject.setMessage("获取随机签名成功");
        returnObject.setResult(goods.getRandomname());
        return returnObject;
    }

    @RequestMapping(value = "/seckill/{goodsId}/{randomName}",method = RequestMethod.GET)
    public @ResponseBody Object seckill(@PathVariable Integer goodsId,@PathVariable String randomName){
        ReturnObject returnObject =  ReturnObject.getInstance();
        if (goodsId==null || randomName == null || randomName.trim().equals("")){
            returnObject.setCode(Contants.ERROR);
            returnObject.setMessage("商品异常！请确认！");
            returnObject.setResult("");
        }

        //用户登录后session中的主键值，用于确认是哪个用户购买了商品
        Integer uid = 1;
        returnObject = goodsService.seckill(goodsId,randomName,uid);
        return returnObject;
    }
}
