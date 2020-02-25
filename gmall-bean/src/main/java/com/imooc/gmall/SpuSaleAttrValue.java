package com.imooc.gmall;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.io.Serializable;

@Data
public class SpuSaleAttrValue implements Serializable{
    @Id
    @Column
    private String id ;

    @Column
    private String spuId;

    @Column
    private String saleAttrId;

    @Column
    private String saleAttrValueName;
    // isChecked 什么用？ 当前的属性值是否被选中！
    @Transient
    private String isChecked;

}

