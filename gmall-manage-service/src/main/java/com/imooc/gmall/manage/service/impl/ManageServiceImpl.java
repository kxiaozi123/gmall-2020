package com.imooc.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.imooc.gmall.*;
import com.imooc.gmall.config.RedisUtil;
import com.imooc.gmall.manage.constant.ManageConst;
import com.imooc.gmall.manage.mapper.*;
import com.imooc.gmall.service.ManageService;
import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ManageServiceImpl implements ManageService {
    @Autowired
    private BaseCatalog1Mapper baseCatalog1Mapper;
    @Autowired
    private BaseCatalog2Mapper baseCatalog2Mapper;
    @Autowired
    private BaseCatalog3Mapper baseCatalog3Mapper;
    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;
    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;
    @Autowired
    private SpuInfoMapper spuInfoMapper;
    @Autowired
    private SpuImageMapper spuImageMapper;
    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;
    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;
    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;
    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;
    @Autowired
    private SkuImageMapper skuImageMapper;
    @Autowired
    private SkuInfoMapper skuInfoMapper;
    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;
    @Autowired
    private RedisUtil redisUtil;

    @Override
    public List<BaseCatalog1> getCatalog1() {
        return baseCatalog1Mapper.selectAll();
    }

    @Override
    public List<BaseCatalog2> getCatalog2(String catalog1Id) {
        BaseCatalog2 baseCatalog2 = new BaseCatalog2();
        baseCatalog2.setCatalog1Id(catalog1Id);
        return baseCatalog2Mapper.select(baseCatalog2);
    }

    @Override
    public List<BaseCatalog3> getCatalog3(String catalog2Id) {
        BaseCatalog3 baseCatalog3 = new BaseCatalog3();
        baseCatalog3.setCatalog2Id(catalog2Id);
        return baseCatalog3Mapper.select(baseCatalog3);
    }

    @Override
    public List<BaseAttrInfo> getAttrList(String catalog3Id) {
        return baseAttrInfoMapper.getBaseAttrInfoListByCatalog3Id(catalog3Id);
    }

    @Transactional
    @Override
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {
        // 修改操作！
        if (baseAttrInfo.getId() != null && baseAttrInfo.getId().length() > 0) {
            baseAttrInfoMapper.updateByPrimaryKeySelective(baseAttrInfo);
        } else {
            // 保存数据 baseAttrInfo
            baseAttrInfoMapper.insertSelective(baseAttrInfo);
        }

        // baseAttrValue ？  先清空数据，在插入到数据即可！
        // 清空数据的条件 根据attrId 为依据
        // delete from baseAttrValue where attrId = baseAttrInfo.getId();
        BaseAttrValue baseAttrValueDel = new BaseAttrValue();
        baseAttrValueDel.setAttrId(baseAttrInfo.getId());
        baseAttrValueMapper.delete(baseAttrValueDel);

        // baseAttrValue
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        // if ( attrValueList.size()>0  && attrValueList!=null){
        if (attrValueList != null && attrValueList.size() > 0) {
            // 循环判断
            for (BaseAttrValue baseAttrValue : attrValueList) {
                //  private String id;
                //  private String valueName; 前台页面传递
                //  private String attrId; attrId=baseAttrInfo.getId();
                //  前提条件baseAttrInfo 对象中的主键必须能够获取到自增的值！
                //  baseAttrValue.setId("122"); 测试事务
                baseAttrValue.setAttrId(baseAttrInfo.getId());
                baseAttrValueMapper.insertSelective(baseAttrValue);
            }
        }
    }

    //没有用到
    @Override
    public List<BaseAttrValue> getAttrValueList(String attrId) {
        BaseAttrValue baseAttrValue = new BaseAttrValue();
        baseAttrValue.setAttrId(attrId);
        return baseAttrValueMapper.select(baseAttrValue);
    }

    //符合业务
    @Override
    public BaseAttrInfo getAttrInfo(String attrId) {
        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectByPrimaryKey(attrId);
        //平台属性值集合
        BaseAttrValue baseAttrValue = new BaseAttrValue();
        baseAttrValue.setAttrId(attrId);
        List<BaseAttrValue> baseAttrValueList = baseAttrValueMapper.select(baseAttrValue);
        baseAttrInfo.setAttrValueList(baseAttrValueList);
        return baseAttrInfo;
    }

    public List<SpuInfo> getSpuList(String catalog3Id) {
        SpuInfo spuInfo = new SpuInfo();
        spuInfo.setCatalog3Id(catalog3Id);
        return spuInfoMapper.select(spuInfo);
    }

    @Transactional
    @Override
    public void saveSpuInfo(SpuInfo spuInfo) {
        spuInfoMapper.insertSelective(spuInfo);
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        if (spuImageList != null && spuImageList.size() > 0) {
            for (SpuImage spuImage : spuImageList) {
                spuImage.setSpuId(spuInfo.getId());
                spuImageMapper.insertSelective(spuImage);
            }
        }
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        if (spuSaleAttrList != null && spuSaleAttrList.size() > 0) {
            for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {
                spuSaleAttr.setSpuId(spuInfo.getId());
                spuSaleAttrMapper.insertSelective(spuSaleAttr);
                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
                if (spuSaleAttrValueList != null && spuSaleAttrValueList.size() > 0) {
                    for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {
                        spuSaleAttrValue.setSpuId(spuInfo.getId());
                        spuSaleAttrValueMapper.insertSelective(spuSaleAttrValue);
                    }
                }
            }
        }

    }

    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {
        return baseSaleAttrMapper.selectAll();
    }

    @Override
    public List<SpuImage> getSpuImageList(String spuId) {
        SpuImage spuImage = new SpuImage();
        spuImage.setSpuId(spuId);
        return spuImageMapper.select(spuImage);
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrList(String spuId) {
        //返回的是List SpuSaleAttr构造的对象（包含SpuSaleAttrValue集合）
        return spuSaleAttrMapper.selectSpuSaleAttrList(spuId);
    }

    @Override
    @Transactional
    public void saveSkuInfo(SkuInfo skuInfo) {
        skuInfoMapper.insertSelective(skuInfo);
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if (skuImageList != null && skuImageList.size() > 0) {
            for (SkuImage skuImage : skuImageList) {
                skuImage.setSkuId(skuInfo.getId());
                skuImageMapper.insertSelective(skuImage);
            }
        }
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if (skuAttrValueList != null && skuAttrValueList.size() > 0) {
            for (SkuAttrValue skuAttrValue : skuAttrValueList) {
                skuAttrValue.setSkuId(skuInfo.getId());
                skuAttrValueMapper.insertSelective(skuAttrValue);
            }
        }
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        if (skuSaleAttrValueList != null && skuSaleAttrValueList.size() > 0) {
            for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {
                skuSaleAttrValue.setSkuId(skuInfo.getId());
                skuSaleAttrValueMapper.insertSelective(skuSaleAttrValue);
            }
        }


    }

    /**
     * 给Item使用 根据SkuId查询SkuInfo对象  未加redis锁的情况
     * (业务代码)
     * @param skuId
     * @return
     */
    //    @Override
//    public SkuInfo getSkuInfo(String skuId) {
//        String skuKey= ManageConst.SKUKEY_PREFIX+skuId+ManageConst.SKUKEY_SUFFIX;
//        Jedis jedis = null;
//        SkuInfo skuInfo;
//        try {
//            jedis = redisUtil.getJedis();
//            if(jedis.exists(skuKey))
//            {
//                String valueJson = jedis.get(skuKey);
//                skuInfo = JSON.parseObject(valueJson, SkuInfo.class);
//                return skuInfo;
//            }
//            else
//            {
//                skuInfo = getSkuInfoDB(skuId);
//                jedis.set(skuKey,JSON.toJSONString(skuInfo));
//                return skuInfo;
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            if(jedis!=null)
//            {
//                jedis.close();
//            }
//        }
//        return getSkuInfoDB(skuId);
//
//
//    }

    /**
     * 给Item使用 根据SkuId查询SkuInfo对象  加了redis锁的情况
     *
     * @param skuId
     * @return
     */
    @Override
    public SkuInfo getSkuInfo(String skuId) {
        return getSkuInfoJedis(skuId);
    }

    /**
     * jedis加锁方式
     * @param skuId
     * @return
     */
    private SkuInfo getSkuInfoJedis(String skuId) {
        // 获取jedis
        SkuInfo skuInfo;
        Jedis jedis = null;
        try {
            jedis = redisUtil.getJedis();
            // 定义key： 见名之意： sku：skuId:info
            String skuKey = ManageConst.SKUKEY_PREFIX+skuId+ManageConst.SKUKEY_SUFFIX;
            String valueJson = jedis.get(skuKey);
            //如果缓存不存在
            if(valueJson==null||valueJson.length()==0)
            {
                String skuLock=ManageConst.SKUKEY_PREFIX+skuId+ManageConst.SKULOCK_SUFFIX;
                String lockKey = jedis.set(skuLock, "good", "NX", "PX", ManageConst.SKULOCK_EXPIRE_PX);
                if("OK".equals(lockKey))
                {
                    skuInfo = getSkuInfoDB(skuId);
                    // 将数据放入缓存
                    String skuRedisStr = JSON.toJSONString(skuInfo);
                    jedis.setex(skuKey,ManageConst.SKUKEY_TIMEOUT,skuRedisStr);
                    // 删除锁！
                    jedis.del(skuLock);
                    return skuInfo;
                }
                else
                {
                    // 等待
                    Thread.sleep(1000);

                    // 调用getSkuInfo();
                    return getSkuInfo(skuId);
                }
            }
            else {
                skuInfo = JSON.parseObject(valueJson, SkuInfo.class);
                return skuInfo;
            }


        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return getSkuInfoDB(skuId);
    }

    /**
     *  RedisSession加锁方式
     * @param skuId
     * @return
     */
    private SkuInfo getSkuInfoRedisson(String skuId) {

        // 放入业务逻辑代码
        SkuInfo skuInfo =null;
        Jedis jedis = null;
        RLock lock = null;
        // ctrl+alt+t
        try {
            Config config = new Config();
            config.useSingleServer().setAddress("redis://212.129.128.36:6379");

            RedissonClient redissonClient = Redisson.create(config);
//        RedissonClient redissonClient = Redisson.create(new Config().useSingleServer().setAddress("redis://192.168.67.219:6379"));

            // 使用redisson 调用getLock
            lock = redissonClient.getLock("yourLock");

            // 加锁
            lock.lock(10, TimeUnit.SECONDS);


            //---------业务代码
            jedis = redisUtil.getJedis();
            // 定义key： 见名之意： sku：skuId:info
            String skuKey = ManageConst.SKUKEY_PREFIX+skuId+ManageConst.SKUKEY_SUFFIX;
            // 判断缓存中是否有数据，如果有，从缓存中获取，没有从db获取并将数据放入缓存！
            // 判断redis 中是否有key
            if (jedis.exists(skuKey)){
                // 取得key 中的value
                String skuJson = jedis.get(skuKey);
                // 将字符串转换为对象
                skuInfo = JSON.parseObject(skuJson, SkuInfo.class);
                //                jedis.close();
                return skuInfo;
            }else {
                skuInfo = getSkuInfoDB(skuId);
                // 放redis 并设置过期时间
                jedis.setex(skuKey,ManageConst.SKUKEY_TIMEOUT,JSON.toJSONString(skuInfo));
                return skuInfo;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 如何解决空指针？  if

            if (jedis!=null){
                jedis.close();
            }
            if (lock!=null){
                lock.unlock();
            }

        }
        return getSkuInfoDB(skuId);
    }



    private SkuInfo getSkuInfoDB(String skuId) {
        SkuInfo skuInfo = skuInfoMapper.selectByPrimaryKey(skuId);
        //并且查出SkuInfoImageList
        SkuImage skuImage = new SkuImage();
        skuImage.setSkuId(skuId);
        List<SkuImage> skuImageList = skuImageMapper.select(skuImage);
        skuInfo.setSkuImageList(skuImageList);
        SkuAttrValue skuAttrValue = new SkuAttrValue();
        skuAttrValue.setSkuId(skuId);
        List<SkuAttrValue> skuAttrValueList = skuAttrValueMapper.select(skuAttrValue);
        skuInfo.setSkuAttrValueList(skuAttrValueList);
        return skuInfo;
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(SkuInfo skuInfo) {
        //根据SkuId和SpuId获取到销售属性(包括销售属性值)
        return spuSaleAttrMapper.selectSpuSaleAttrListCheckBySku(skuInfo.getId(), skuInfo.getSpuId());
    }

    @Override
    public List<SkuSaleAttrValue> getSkuSaleAttrValueListBySpu(String spuId) {
        // 根据spuId 获取到Sku销售属性值的集合
        return skuSaleAttrValueMapper.selectSkuSaleAttrValueListBySpu(spuId);
    }

    @Override
    public List<BaseAttrInfo> getAttrList(List<String> attrValueIdList) {
        String valueIds = StringUtils.join(attrValueIdList.toArray(), ",");
        return baseAttrInfoMapper.selectAttrInfoListByIds(valueIds);

    }




}
