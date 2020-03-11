package com.imooc.gmall.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.imooc.gmall.beans.OrderInfo;
import com.imooc.gmall.beans.PaymentInfo;
import com.imooc.gmall.enums.PaymentStatus;
import com.imooc.gmall.payment.config.AlipayConfig;
import com.imooc.gmall.service.OrderService;
import com.imooc.gmall.service.PaymentSerivce;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Controller
public class PaymentController {
    @Reference
    private OrderService orderService;

    @Reference
    private PaymentSerivce paymentSerivce;

    @Autowired
    private AlipayClient alipayClient;

    @RequestMapping("index")
    public String index(String orderId, HttpServletRequest request){
        // 选中支付渠道！
        // 获取总金额 通过orderId 获取订单的总金额
        OrderInfo orderInfo =  orderService.getOrderInfo(orderId);

        // 保存订单Id
        request.setAttribute("orderId",orderId);
        // 保存订单总金额
        request.setAttribute("totalAmount",orderInfo.getTotalAmount());

        return "index";
    }
    @RequestMapping("alipay/submit")
    @ResponseBody
    public String alipaySubmit(HttpServletRequest request, HttpServletResponse response){
        /*
            1.  保存支付记录下 将数据放入数据库
                去重复，对账！ 幂等性=保证每一笔交易只能交易一次 {第三方交易编号outTradeNo}！
                paymentInfo
            2.  生成二维码
         */
        // 获取orderId
        String orderId = request.getParameter("orderId");
        // 通过orderId 将数据查询出来
        OrderInfo orderInfo =  orderService.getOrderInfo(orderId);

        PaymentInfo paymentInfo = new PaymentInfo();
        // paymentInfo 数据来源于谁！orderInfo

        // 属性赋值！
        paymentInfo.setOrderId(orderId);
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        paymentInfo.setSubject("给韩鹏买xxx！");
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID);
        paymentInfo.setCreateTime(new Date());

        paymentSerivce.savePaymentInfo(paymentInfo);

        // 生成二维码！
        // 参数做成配置文件，进行软编码！
        // AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do", APP_ID, APP_PRIVATE_KEY, FORMAT, CHARSET, ALIPAY_PUBLIC_KEY, SIGN_TYPE); //获得初始化的AlipayClient
        // alipay.trade.page.pay
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();//创建API对应的request
        // 设置同步回调
//        alipayRequest.setReturnUrl("http://domain.com/CallBack/return_url.jsp");
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        // 设置异步回调
        // alipayRequest.setNotifyUrl("http://domain.com/CallBack/notify_url.jsp");//在公共参数中设置回跳和通知地址
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);
        // 参数
        // 声明一个map 集合来存储参数
        HashMap<String, Object> map = new HashMap<>();
        map.put("out_trade_no",paymentInfo.getOutTradeNo());
        map.put("product_code","FAST_INSTANT_TRADE_PAY");
        map.put("total_amount",paymentInfo.getTotalAmount());
        map.put("subject",paymentInfo.getSubject());
        // 将封装好的参数传递给支付宝！
        alipayRequest.setBizContent(JSON.toJSONString(map));

        String form="";
        try {
            form = alipayClient.pageExecute(alipayRequest).getBody(); //调用SDK生成表单
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        response.setContentType("text/html;charset=UTF-8");

        // 调用延迟队列
        paymentSerivce.sendDelayPaymentResult(paymentInfo.getOutTradeNo(),15,3);
        return form;
    }
    // http://payment.gmall.com/alipay/callback/return
    // 付款完成之后，订单，购物车数据应该情况！
    //
    @RequestMapping("alipay/callback/return")
    public String callbackReturn(){
        return "redirect:"+AlipayConfig.return_order_url;
    }
    // 异步回调
    @RequestMapping("alipay/callback/notify")
    public String callbackNotify(@RequestParam Map<String,String> paramMap){
        // Map<String, String> paramsMap = ... //将异步通知中收到的所有参数都存放到map中
        boolean flag = false; //调用SDK验证签名
        try {
            flag = AlipaySignature.rsaCheckV1(paramMap, AlipayConfig.alipay_public_key, AlipayConfig.charset, AlipayConfig.sign_type);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }

        if(flag){
            // TODO 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
            // 对业务的二次校验
            // 只有交易通知状态为 TRADE_SUCCESS 或 TRADE_FINISHED 时，支付宝才会认定为买家付款成功。
            // 支付成功之后，需要做什么？
            // 需要得到trade_status
            String trade_status = paramMap.get("trade_status");
            // 通过out_trade_no 查询支付状态记录
            String out_trade_no = paramMap.get("out_trade_no");

            // String total_amount = paramMap.get("total_amount");
            if ("TRADE_SUCCESS".equals(trade_status) || "TRADE_FINISHED".equals(trade_status)){
                // 当前的订单支付状态如果是已经付款，或者是关闭
                // select * from paymentInfo where out_trade_no =?
                PaymentInfo paymentInfoQuery = new PaymentInfo();
                paymentInfoQuery.setOutTradeNo(out_trade_no);
                PaymentInfo paymentInfo = paymentSerivce.getPaymentInfo(paymentInfoQuery);

                if (paymentInfo.getPaymentStatus()==PaymentStatus.PAID || paymentInfo.getPaymentStatus()==PaymentStatus.ClOSED){
                    return "failure";
                }

                // 更新交易记录的状态！
                // update paymentInfo set PaymentStatus=PaymentStatus.PAID , callbackTime = new Date() where out_trade_no=?

                PaymentInfo paymentInfoUPD = new PaymentInfo();
                paymentInfoUPD.setPaymentStatus(PaymentStatus.PAID);
                paymentInfoUPD.setCallbackTime(new Date());

                paymentSerivce.updatePaymentInfo(out_trade_no,paymentInfoUPD);
                // 发送消息队列给订单：orderId, result
                paymentSerivce.sendPaymentResult(paymentInfo,"success");
                return "success";
            }
        }else{
            // TODO 验签失败则记录异常日志，并在response中返回failure.
            return "failure";
        }
        return "failure";
    }

    // http://payment.gmall.com/refund?orderId=100
    //输入url直接调用支付宝接口退款
    @RequestMapping("refund")
    @ResponseBody
    public String refund(String orderId){
        // 退款接口
        boolean result =  paymentSerivce.refund(orderId);
        return ""+result;
    }

    // 根据orderId 支付
    @RequestMapping("wx/submit")
    @ResponseBody
    public Map<String, String> wxSubmit(String orderId){


        // orderId 订单编号，1 表示金额 分
        orderId = UUID.randomUUID().toString().replace("-","");
        Map<String, String> map = paymentSerivce.createNative(orderId,"1");
        System.out.println(map.get("code_url"));
        // map中必须有code_url
        return map;
    }
}
