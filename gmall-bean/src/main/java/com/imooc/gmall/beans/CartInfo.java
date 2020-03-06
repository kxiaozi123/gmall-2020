package com.imooc.gmall.beans;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
@Data
public class CartInfo implements Serializable {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column
    private String id;
    @Column
    private String userId;
    @Column
    private String skuId;
    @Column
    private BigDecimal cartPrice;
    @Column
    private Integer skuNum;
    @Column
    private String imgUrl;
    @Column
    private String skuName;

    // 实时价格
    @Transient
    BigDecimal skuPrice;
    // 下订单的时候，商品是否勾选
    @Transient
    String isChecked="0";

}
