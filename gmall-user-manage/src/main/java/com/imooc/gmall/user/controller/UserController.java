package com.imooc.gmall.user.controller;

import com.imooc.gmall.beans.UserInfo;
import com.imooc.gmall.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class UserController {
    @Autowired
    private UserService service;
    @GetMapping("/findAll")
    public List<UserInfo> findAll()
    {
        return service.findAll();
    }
}
