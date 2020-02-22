package com.imooc.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.imooc.gmall.UserAddress;
import com.imooc.gmall.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class OrderController {
    @Reference
    private UserService userService;
    @GetMapping("/trade")
    public List<UserAddress> trade(String id)
    {
      return userService.findAddressByUserId(id);
    }
}
