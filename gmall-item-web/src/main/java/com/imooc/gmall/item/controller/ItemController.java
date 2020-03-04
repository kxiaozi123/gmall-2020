package com.imooc.gmall.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.imooc.gmall.SkuInfo;
import com.imooc.gmall.SkuSaleAttrValue;
import com.imooc.gmall.SpuSaleAttr;
import com.imooc.gmall.config.LoginRequire;
import com.imooc.gmall.service.ListService;
import com.imooc.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ItemController {
    @Reference
    private ManageService manageService;
    @Reference
    private ListService listService;
    @RequestMapping("{skuId}.html")
    @LoginRequire
    public String item(@PathVariable String skuId, HttpServletRequest request){
        //根据SkuId获取SkuInfo(带SkuImageList)
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);

        //根据SkuInfo获取到SkuId和SpuId再去获取销售属性（带销售属性值）的集合
        //回显并锁定销售属性值
        List<SpuSaleAttr> spuSaleAttrList = manageService.getSpuSaleAttrListCheckBySku(skuInfo);

        //根据SkuInfo中的SpuId   获取到Sku销售属性值的集合
        // 用来判断用户选择了哪些属性 再去选择SkuId
        //点击销售属性值实现切换功能
        List<SkuSaleAttrValue> skuSaleAttrValueList = manageService.getSkuSaleAttrValueListBySpu(skuInfo.getSpuId());
        Map<String,Object> map=new HashMap<>();
        String key="";
        //132 132| 132|134
        for (int i = 0; i < skuSaleAttrValueList.size(); i++) {
            SkuSaleAttrValue skuSaleAttrValue = skuSaleAttrValueList.get(i);

            if(key.length()>0)
            {
                key+="|";
            }
            key+=skuSaleAttrValue.getSaleAttrValueId();
            if((i+1)==skuSaleAttrValueList.size()||!skuSaleAttrValue.getSkuId().equals(skuSaleAttrValueList.get(i+1).getSkuId()))
            {
                map.put(key,skuSaleAttrValue.getSkuId());
                key="";
            }
        }
        String valuesSkuJson = JSON.toJSONString(map);
        //System.out.println("拼接Json：="+valuesSkuJson );
        // 保存json
        request.setAttribute("valuesSkuJson",valuesSkuJson);
        request.setAttribute("skuInfo",skuInfo);
        request.setAttribute("spuSaleAttrList",spuSaleAttrList);
        listService.incrHotScore(skuId);
        return "item";
    }

}
