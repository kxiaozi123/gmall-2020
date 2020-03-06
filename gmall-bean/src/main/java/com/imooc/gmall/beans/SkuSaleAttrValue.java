package com.imooc.gmall.beans;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Id;
import java.io.Serializable;

@Data
public class SkuSaleAttrValue implements Serializable{
    @Id
    @Column
    private String id;

    @Column
    private String skuId;

    @Column
    private String saleAttrId;

    @Column
    private String saleAttrValueId;

    @Column
    private String saleAttrName;

    @Column
    private String saleAttrValueName;


}
