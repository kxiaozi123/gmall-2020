package com.imooc.gmall.beans;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class SkuLsInfo implements Serializable {

    // 不加注解是因为不是数据库的表
    private String id;

    private BigDecimal price;

    private String skuName;

    private String catalog3Id;

    private String skuDefaultImg;

    // 自定义一个字段来保存热度评分
    private Long hotScore=0L;

    private List<SkuLsAttrValue> skuAttrValueList;

}
