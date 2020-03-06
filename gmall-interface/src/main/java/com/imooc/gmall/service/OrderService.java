package com.imooc.gmall.service;

import com.imooc.gmall.beans.OrderInfo;

public interface OrderService {
    //// 生成流水号
    String getTradeNo(String userId);
    //保存订单
    String  saveOrder(OrderInfo orderInfo);
    //检查tradeCode
    Boolean checkTradeCode(String userId, String tradeNo);
    //删除tradeCode
    void delTradeNo(String userId);
}
