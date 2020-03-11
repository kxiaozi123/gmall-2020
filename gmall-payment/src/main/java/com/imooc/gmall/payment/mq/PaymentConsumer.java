package com.imooc.gmall.payment.mq;

import com.alibaba.dubbo.config.annotation.Reference;
import com.imooc.gmall.beans.PaymentInfo;
import com.imooc.gmall.service.PaymentSerivce;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;

@Component
public class PaymentConsumer {

    @Reference
    private PaymentSerivce paymentSerivce;

    // 消费检查是否支付成功的消息队列
    @JmsListener(destination = "PAYMENT_RESULT_CHECK_QUEUE",containerFactory = "jmsQueueListener")
    public void consumeSkuDeduct(MapMessage mapMessage) throws JMSException {
        // 通过mapMessage获取
        String outTradeNo = mapMessage.getString("outTradeNo");
        int delaySec = mapMessage.getInt("delaySec");
        int checkCount = mapMessage.getInt("checkCount");

        // 创建一个paymentInfo 对象
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOutTradeNo(outTradeNo);
        // 获取orderId
        PaymentInfo paymentInfoQuery = paymentSerivce.getPaymentInfo(paymentInfo);

        // 其他参数没有值？
        // 判断是否支付成功！
        boolean result = paymentSerivce.checkPayment(paymentInfoQuery);
        System.out.println("检查结果："+result);
        // 支付失败！
        if (!result && checkCount>0){
            System.out.println("检查次数："+checkCount);
            // 调用发送消息的方法即可！
            paymentSerivce.sendDelayPaymentResult(outTradeNo,delaySec,checkCount-1);

        }

    }
}
