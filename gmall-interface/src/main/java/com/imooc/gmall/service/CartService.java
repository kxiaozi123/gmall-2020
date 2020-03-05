package com.imooc.gmall.service;

import com.imooc.gmall.CartInfo;

import java.util.List;

public interface CartService {
    void  addToCart(String skuId,String userId,Integer skuNum);

    List<CartInfo> getCartList(String userId);

    List<CartInfo> mergeToCartList(List<CartInfo> cartListCK, String userId);
}
