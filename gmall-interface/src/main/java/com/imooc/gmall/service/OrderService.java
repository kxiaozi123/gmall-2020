package com.imooc.gmall.service;

import com.imooc.gmall.beans.OrderInfo;
import com.imooc.gmall.enums.ProcessStatus;

import java.util.List;
import java.util.Map;

public interface OrderService {
    //// 生成流水号
    String getTradeNo(String userId);
    //保存订单
    String  saveOrder(OrderInfo orderInfo);
    //检查tradeCode
    boolean checkTradeCode(String userId, String tradeNo);
    //删除tradeCode
    void delTradeNo(String userId);

    boolean checkStock(String skuId, Integer skuNum);

    OrderInfo getOrderInfo(String orderId);

    void updateOrderStatus(String orderId, ProcessStatus paid);

    void sendOrderStatus(String orderId);

    List<OrderInfo> getExpiredOrderList();

    void execExpiredOrder(OrderInfo orderInfo);

    List<OrderInfo> orderSplit(String orderId, String wareSkuMap);

    Map<String, Object> initWareOrder(OrderInfo orderInfo);
}
