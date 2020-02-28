package com.imooc.gmall.list.controller;

import com.alibaba.fastjson.JSON;
import com.imooc.gmall.SkuLsParams;
import com.imooc.gmall.SkuLsResult;
import com.imooc.gmall.service.ListService;
import jdk.nashorn.internal.ir.annotations.Reference;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ListController {
    @Reference
    private ListService listService;
    // http://list.gmall.com/list.html?catalog3Id=61
    @RequestMapping("list.html")
    public String listData(SkuLsParams skuLsParams){

        SkuLsResult skuLsResult = listService.search(skuLsParams);

        return JSON.toJSONString(skuLsResult);
    }
}
