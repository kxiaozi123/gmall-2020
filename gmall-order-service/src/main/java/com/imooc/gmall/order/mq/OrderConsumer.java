package com.imooc.gmall.order.mq;

import com.alibaba.dubbo.config.annotation.Reference;
import com.imooc.gmall.enums.ProcessStatus;
import com.imooc.gmall.service.OrderService;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;

@Component
public class OrderConsumer {
    @Reference
    private OrderService orderService;
    // 获取消息队列中的数据
    /**
     * destination 表示监听的队列名称
     */
    //订单系统接受到支付系统的消息 然后更新订单状态 再通知库存系统 再把订单状态改成 已通知仓储
    @JmsListener(destination = "PAYMENT_RESULT_QUEUE",containerFactory = "jmsQueueListener")
    public void consumerPaymentResult(MapMessage mapMessage) throws JMSException {
        // 通过mapMessage获取
        String orderId = mapMessage.getString("orderId");
        String result = mapMessage.getString("result");
        // 支付成功
        if ("success".equals(result)){
            // 更新订单状态
            orderService.updateOrderStatus(orderId, ProcessStatus.PAID);
            // 发送消息给库存
            orderService.sendOrderStatus(orderId);
            // 更新订单状态
            orderService.updateOrderStatus(orderId, ProcessStatus.NOTIFIED_WARE);
        }
    }
    //库存系统 扣完库存发来的结果
    @JmsListener(destination = "SKU_DEDUCT_QUEUE",containerFactory = "jmsQueueListener")
    public void consumeSkuDeduct(MapMessage mapMessage) throws JMSException {
        // 通过mapMessage获取
        String orderId = mapMessage.getString("orderId");
        String status = mapMessage.getString("status");
        if ("DEDUCTED".equals(status)){
            orderService.updateOrderStatus(orderId, ProcessStatus.DELEVERED);
        }
    }
}
