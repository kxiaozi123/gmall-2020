package com.imooc.gmall.order.controller;
import com.alibaba.dubbo.config.annotation.Reference;
import com.imooc.gmall.beans.CartInfo;
import com.imooc.gmall.beans.OrderDetail;
import com.imooc.gmall.beans.OrderInfo;
import com.imooc.gmall.beans.UserAddress;
import com.imooc.gmall.config.LoginRequire;
import com.imooc.gmall.enums.OrderStatus;
import com.imooc.gmall.enums.ProcessStatus;
import com.imooc.gmall.service.CartService;
import com.imooc.gmall.service.OrderService;
import com.imooc.gmall.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

@Controller
public class OrderController {
    @Reference
    private UserService userService;
    @Reference
    private CartService cartService;
    @Reference
    private OrderService orderService;

    @RequestMapping(value = "trade", method = RequestMethod.GET)
    @LoginRequire
    public String tradeInit(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        // 收货人地址
        List<UserAddress> userAddressList = userService.findAddressByUserId(userId);
        request.setAttribute("userAddressList",userAddressList);
        // 声明一个集合来存储订单明细
        List<OrderDetail> orderDetailArrayList = new ArrayList<>();
        // 得到选中的购物车列表
        List<CartInfo> cartCheckedList = cartService.getCartCheckedList(userId);

        for (CartInfo cartInfo : cartCheckedList) {
            OrderDetail orderDetail = new OrderDetail();

            orderDetail.setSkuId(cartInfo.getSkuId());
            orderDetail.setSkuName(cartInfo.getSkuName());
            orderDetail.setImgUrl(cartInfo.getImgUrl());
            orderDetail.setSkuNum(cartInfo.getSkuNum());
            orderDetail.setOrderPrice(cartInfo.getCartPrice());

            orderDetailArrayList.add(orderDetail);
        }
        // 总金额：
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderDetailList(orderDetailArrayList);
        // 调用计算总金额的方法  {totalAmount}
        orderInfo.sumTotalAmount();
        request.setAttribute("totalAmount",orderInfo.getTotalAmount());
        // 保存送货清单集合
        request.setAttribute("orderDetailArrayList",orderDetailArrayList);

        String tradeNo = orderService.getTradeNo(userId);
        request.setAttribute("tradeNo",tradeNo);
        return  "trade";
    }
    @RequestMapping(value = "submitOrder",method = RequestMethod.POST)
    @LoginRequire
    public String submitOrder(OrderInfo orderInfo,HttpServletRequest request){
        // 检查tradeCode
        String userId = (String) request.getAttribute("userId");
        // 检查tradeCode
        String tradeNo = request.getParameter("tradeNo");
        boolean flag = orderService.checkTradeCode(userId, tradeNo);
        if (!flag){
            request.setAttribute("errMsg","该页面已失效，请重新结算!");
            return "tradeFail";
        }
        // 初始化参数
        orderInfo.setOrderStatus(OrderStatus.UNPAID);
        orderInfo.setProcessStatus(ProcessStatus.UNPAID);
        orderInfo.sumTotalAmount();
        orderInfo.setUserId(userId);
        // 保存订单
        String orderId = orderService.saveOrder(orderInfo);
        // 删除tradeNo
        orderService.delTradeNo(userId);
        // 重定向
        return "redirect://payment.gmall.com/index?orderId="+orderId;
    }

}
