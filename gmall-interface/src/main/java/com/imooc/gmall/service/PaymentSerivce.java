package com.imooc.gmall.service;

import com.imooc.gmall.beans.PaymentInfo;

import java.util.Map;

public interface PaymentSerivce {
    void savePaymentInfo(PaymentInfo paymentInfo);

    void sendDelayPaymentResult(String outTradeNo, int delaySec, int checkCount);

    PaymentInfo getPaymentInfo(PaymentInfo paymentInfoQuery);

    void updatePaymentInfo(String out_trade_no, PaymentInfo paymentInfoUPD);

    void sendPaymentResult(PaymentInfo paymentInfo, String success);

    boolean refund(String orderId);

    Map<String, String> createNative(String orderId, String s);

    boolean checkPayment(PaymentInfo paymentInfoQuery);

    void closePayment(String orderId);
}
