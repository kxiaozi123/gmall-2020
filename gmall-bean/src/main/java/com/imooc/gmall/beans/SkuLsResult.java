package com.imooc.gmall.beans;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class SkuLsResult implements Serializable {

    private List<SkuLsInfo> skuLsInfoList;

    private long total;

    private long totalPages;

    private List<String> attrValueIdList;

}
