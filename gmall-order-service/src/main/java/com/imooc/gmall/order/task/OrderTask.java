package com.imooc.gmall.order.task;

import com.alibaba.dubbo.config.annotation.Reference;
import com.imooc.gmall.beans.OrderInfo;
import com.imooc.gmall.service.OrderService;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@EnableScheduling
@Component
public class OrderTask {

    @Reference
    private OrderService orderService;
    // cron 表示任务启动规则
    // 每分钟的第五秒执行该方法
    @Scheduled(cron = "5 * * * * ?")
    public void test01(){
        System.out.println(Thread.currentThread().getName()+"---------------0001---------------");
    }
    // 每隔五秒执行一次
    @Scheduled(cron ="0/5 * * * * ?")
    public void test02(){
        System.out.println(Thread.currentThread().getName()+"---------------0002---------------");
    }

    @Scheduled(cron ="0/20 * * * * ?")
    public void checkOrder(){
       /*
       1.	查询有多少订单是过期：
            什么样的订单算是过期了？
            当前系统时间>过期时间 and 当前状态是未支付！

        2.	循环过期订单列表，进行处理！
            orderInfo
            paymentInfo
        */
        List<OrderInfo> orderInfoList =  orderService.getExpiredOrderList();

        for (OrderInfo orderInfo : orderInfoList) {
            // 关闭过期订单
            orderService.execExpiredOrder(orderInfo);
        }

    }
}
