package com.imooc.gmall.user.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.imooc.gmall.UserAddress;
import com.imooc.gmall.UserInfo;
import com.imooc.gmall.service.UserService;
import com.imooc.gmall.user.mapper.UserAddressMapper;
import com.imooc.gmall.user.mapper.UserInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private UserAddressMapper userAddressMapper;

    @Override
    public List<UserInfo> findAll() {
        return userInfoMapper.selectAll();
    }

    @Override
    public List<UserAddress> findAddressByUserId(String id) {
        UserAddress userAddress = new UserAddress();
        userAddress.setUserId(id);
        return userAddressMapper.select(userAddress);
    }
}
