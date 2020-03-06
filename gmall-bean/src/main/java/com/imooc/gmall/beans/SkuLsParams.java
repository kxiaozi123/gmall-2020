package com.imooc.gmall.beans;

import lombok.Data;

import java.io.Serializable;

@Data
public class SkuLsParams implements Serializable {

    // keyword = skuName

    private String  keyword;

    private String catalog3Id;

    private String[] valueId;

    private int pageNo=1;

    private int pageSize=20;

}
