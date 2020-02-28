package com.imooc.gmall.service;

import com.imooc.gmall.SkuLsInfo;
import com.imooc.gmall.SkuLsParams;
import com.imooc.gmall.SkuLsResult;

public interface ListService {

    /**
     * 保存数据到es 中！
     * @param skuLsInfo
     */
    void saveSkuLsInfo(SkuLsInfo skuLsInfo);

    /**
     * 查询
     * @param skuLsParams
     * @return
     */
    SkuLsResult search(SkuLsParams skuLsParams);
}
