package com.imooc.gmall.service;

import com.imooc.gmall.UserAddress;
import com.imooc.gmall.UserInfo;

import java.util.List;

public interface UserService {
    List<UserInfo> findAll();

    List<UserAddress> findAddressByUserId(String id);

    UserInfo login(UserInfo userInfo);

    UserInfo verify(String userId);
}
