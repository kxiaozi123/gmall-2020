package com.imooc.gmall.beans;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Id;
import java.io.Serializable;

@Data
public class SkuImage implements Serializable {
    @Id
    @Column
    private String id;
    @Column
    private String skuId;
    @Column
    private String imgName;
    @Column
    private String imgUrl;
    @Column
    private String spuImgId;
    @Column
    private String isDefault;

}
