package com.bjpowernode.common;

import java.io.Serializable;

/**
 * @ClassName ReturnObject
 * @Description: TODO
 * @Author codemi@aliyun.com
 * @Date 2019/11/22
 **/
public final class ReturnObject implements Serializable {

    private String code;

    private String message;

    private Object result;

    private ReturnObject(){}

    private static ReturnObject returnObject = new ReturnObject();

    public static ReturnObject getInstance(){

        return returnObject;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

}
