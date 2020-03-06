package com.imooc.gmall.service;

import com.imooc.gmall.beans.UserAddress;
import com.imooc.gmall.beans.UserInfo;

import java.util.List;

public interface UserService {
    List<UserInfo> findAll();

    List<UserAddress> findAddressByUserId(String id);

    UserInfo login(UserInfo userInfo);

    UserInfo verify(String userId);
}
