package com.imooc.gmall.manage.mapper;

import com.imooc.gmall.beans.SkuSaleAttrValue;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface SkuSaleAttrValueMapper extends Mapper<SkuSaleAttrValue>{
    List<SkuSaleAttrValue> selectSkuSaleAttrValueListBySpu(String spuId);
}
