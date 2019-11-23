package com.bjpowernode.common;

/**
 * @ClassName Contants
 * @Description: TODO
 * @Author codemi@aliyun.com
 * @Date 2019/11/22
 **/
public final class Contants {
    private Contants() {
    }

    public static final String OK = "0";

    public static final String ERROR = "1";


    public static final String GOODS_STORE = "goods_store";

    /**
     * 秒杀限购前缀
     */
    public static final String PURCHASE_LIMITS = "purchase_limits";

    /**
     * 限流人数的key
     */
    public static final String CURRENT_LIMITING = "current_limiting";

    /**
     * redis中的订单前缀
     */
    public static final String ORDERS = "orders";

    public static final String ORDER_RESULT = "order_result";
}
