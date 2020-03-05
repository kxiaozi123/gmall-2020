package com.imooc.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.imooc.gmall.CartInfo;
import com.imooc.gmall.SkuInfo;
import com.imooc.gmall.cart.constant.CartConst;
import com.imooc.gmall.cart.mapper.CartInfoMapper;
import com.imooc.gmall.config.RedisUtil;
import com.imooc.gmall.service.CartService;
import com.imooc.gmall.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

@Service
public class CartServiceImpl implements CartService {
    @Autowired
    private CartInfoMapper cartInfoMapper;
    @Reference
    private ManageService manageService;
    @Autowired
    private RedisUtil redisUtil;

    @Override
    public void addToCart(String skuId, String userId, Integer skuNum) {
        // 构建key user:userid:cart
        String userCartKey = CartConst.USER_KEY_PREFIX+userId+ CartConst.USER_CART_KEY_SUFFIX;
        CartInfo cartInfo = new CartInfo();
        cartInfo.setUserId(userId);
        cartInfo.setSkuId(skuId);
        //根据用户Id和SkuId查询购物车
        CartInfo cartInfoExist  = cartInfoMapper.selectOne(cartInfo);
        if (cartInfoExist != null) {
            //更新数量
            cartInfoExist.setSkuNum(cartInfoExist.getSkuNum()+skuNum);
            cartInfoExist.setSkuPrice(cartInfoExist.getCartPrice());
            cartInfoMapper.updateByPrimaryKeySelective(cartInfoExist);
        }
        else {
            //添加购物车
            // 如果不存在，保存购物车
            SkuInfo skuInfo = manageService.getSkuInfo(skuId);
            CartInfo cartInfo1 = new CartInfo();
            cartInfo1.setSkuId(skuId);
            cartInfo1.setCartPrice(skuInfo.getPrice());
            cartInfo1.setSkuPrice(skuInfo.getPrice());
            cartInfo1.setSkuName(skuInfo.getSkuName());
            cartInfo1.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo1.setUserId(userId);
            cartInfo1.setSkuNum(skuNum);
            // 插入数据库
            cartInfoMapper.insertSelective(cartInfo1);

            cartInfoExist=cartInfo1;

        }
        // 将对象序列化
        String cartJson = JSON.toJSONString(cartInfoExist);
        // 准备取数据
        Jedis jedis = redisUtil.getJedis();
        // 构建key user:userid:cart 37
        jedis.hset(userCartKey,skuId,cartJson);
        // 更新购物车过期时间 user: id:info
        String userInfoKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USERINFOKEY_SUFFIX;
        Long ttl = jedis.ttl(userInfoKey);
        jedis.expire(userCartKey,ttl.intValue());
        jedis.close();

    }

    @Override
    public List<CartInfo> getCartList(String userId) {
        List<CartInfo> cartInfoList = new ArrayList<>();
        // 缓存中没有数据，则从 数据库中加载
        // 从redis中取得，
        Jedis jedis = redisUtil.getJedis();
        String userCartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        List<String> cartJsons = jedis.hvals(userCartKey);
        if (cartJsons != null&&cartJsons.size()>0) {
            //把缓存中的String JSon串集合加到新的集合中 然后根据cartId排序
            for (String cartJson : cartJsons) {
                cartInfoList.add(JSON.parseObject(cartJson,CartInfo.class));
            }
            cartInfoList.sort(Comparator.comparing(CartInfo::getId));
            return cartInfoList;
        }
        else {
            // 从数据库获取数据 order by ,并添加到缓存！
            cartInfoList = loadCartCache(userId);
            return cartInfoList;

        }
    }

    @Override
    public List<CartInfo> mergeToCartList(List<CartInfo> cartListCK, String userId) {
        /*
            未登录： 33 1 ， 34 2
            登录：34 1 ，36 1
            匹配之后：33 1 ，34 3
            合并 ：33 1 ，34 3，36 1
         */

        // 根据userId 获取购物车数据
        List<CartInfo> cartInfoListDB = cartInfoMapper.selectCartListWithCurPrice(userId);
        for (CartInfo cartInfoCK : cartListCK) {
            // 定义一个boolean 类型变量 默认值给false
            boolean isMatch =false;
            for (CartInfo cartInfoDB : cartInfoListDB) {
                if(cartInfoCK.getSkuId().equals(cartInfoDB.getSkuId()))
                {
                    // 将数量进行相加
                    cartInfoDB.setSkuNum(cartInfoCK.getSkuNum()+cartInfoDB.getSkuNum());
                    // 修改数据库
                    cartInfoMapper.updateByPrimaryKeySelective(cartInfoDB);
                    isMatch=true;
                }
            }
            // 没有匹配上！
            if (!isMatch){
                // 未登录的对象添加到数据库
                // 将用户Id 赋值给未登录对象
                cartInfoCK.setUserId(userId);
                cartInfoMapper.insertSelective(cartInfoCK);
            }
        }
        // 最终将合并之后的数据返回！  最后再查一下
        List<CartInfo> cartInfoList = loadCartCache(userId);

        return cartInfoList;
    }

    //  // 根据userId 查询购物车 {skuPrice 实时价格}
    //查找数据库 然后设置到缓存中
    private List<CartInfo> loadCartCache(String userId) {
        // select * from cartInfo where userId = ? 不可取！查询不到实时价格！
        // cartInfo , skuInfo 从这两张表中查询！
        List<CartInfo> cartInfoList = cartInfoMapper.selectCartListWithCurPrice(userId);
        if (cartInfoList==null || cartInfoList.size()==0){
            return  null;
        }
        // 将结果放到缓存众 获取jedis
        Jedis jedis = redisUtil.getJedis();
        // 定义购物车的key=user:userId:cart  用户key=user:userId:info
        String cartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;

        // cartInfoList 从数据库查询到的数据放入 redis
//        for (CartInfo cartInfo : cartInfoList) {
//            jedis.hset(cartKey,cartInfo.getSkuId(),JSON.toJSONString(cartInfo));
//        }
        //这种性能比较好
        HashMap<String, String> map = new HashMap<>();
        for (CartInfo cartInfo : cartInfoList) {
            map.put(cartInfo.getSkuId(),JSON.toJSONString(cartInfo));
        }

        // 一次放入多条数据
        jedis.hmset(cartKey,map);
        jedis.close();
        return cartInfoList;
    }
}
