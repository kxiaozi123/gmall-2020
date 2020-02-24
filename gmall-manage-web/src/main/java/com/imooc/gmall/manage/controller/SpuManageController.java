package com.imooc.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.imooc.gmall.SpuInfo;
import com.imooc.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
@Controller
@CrossOrigin
public class SpuManageController {
    @Reference
    private ManageService manageService;

    // http://localhost:8082/spuList?catalog3Id=63 实体类对象封装
    @RequestMapping("spuList")
    public List<SpuInfo> spuList(SpuInfo spuInfo){
        return manageService.getSpuList(spuInfo);
    }

}
