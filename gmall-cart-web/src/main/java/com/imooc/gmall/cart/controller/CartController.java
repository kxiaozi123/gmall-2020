package com.imooc.gmall.cart.controller;
import com.alibaba.dubbo.config.annotation.Reference;
import com.imooc.gmall.CartInfo;
import com.imooc.gmall.SkuInfo;
import com.imooc.gmall.config.LoginRequire;
import com.imooc.gmall.service.CartService;
import com.imooc.gmall.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Controller
public class CartController {
    @Reference
    private CartService cartService;
    @Autowired
    private CartCookieHandler cartCookieHandler;
    @Reference
    private ManageService manageService;

    @RequestMapping("addToCart")
    @LoginRequire(autoRedirect = false)
    public String addToCart(HttpServletRequest request, HttpServletResponse response) {
        // 获取userId，skuId，skuNum
        String skuId = request.getParameter("skuId");
        String skuNum = request.getParameter("skuNum");

        //获取userId
        String userId = (String) request.getAttribute("userId");
        if (userId != null) {
            // 说明用户登录
            cartService.addToCart(skuId, userId, Integer.parseInt(skuNum));
        } else {
            // 说明用户没有登录没有登录放到cookie中
            cartCookieHandler.addToCart(request, response, skuId, userId, Integer.parseInt(skuNum));
        }
        // 取得sku信息对象
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        request.setAttribute("skuInfo", skuInfo);
        request.setAttribute("skuNum", skuNum);

        return "success";

    }

    @RequestMapping("cartList")
    @LoginRequire(autoRedirect = false)
    public String cartList(HttpServletRequest request, HttpServletResponse response) {
        // 获取userId
        String userId = (String) request.getAttribute("userId");
        List<CartInfo> cartInfoList = null;
        if (userId != null) { //用户登陆
            // 合并购物车：
            List<CartInfo> cartListCK = cartCookieHandler.getCartList(request);
            //cartListCK是cookie中的数据 如果存在 加到用户购物车中，否则就还是用户原来的购物车
            if (cartListCK!=null && cartListCK.size()>0){
                // 合并购物车
                cartInfoList = cartService.mergeToCartList(cartListCK,userId);
            }else {
                // 登录状态下查询购物车
                cartInfoList = cartService.getCartList(userId);
            }

        } else {
            // 调用未登录添加购物车
            cartInfoList = cartCookieHandler.getCartList(request);
        }
        // 保存购物车集合
        request.setAttribute("cartInfoList", cartInfoList);
        return "cartList";
    }
}
