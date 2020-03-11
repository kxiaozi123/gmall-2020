package com.imooc.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.imooc.gmall.beans.OrderDetail;
import com.imooc.gmall.beans.OrderInfo;
import com.imooc.gmall.config.ActiveMQUtil;
import com.imooc.gmall.config.RedisUtil;
import com.imooc.gmall.enums.ProcessStatus;
import com.imooc.gmall.order.mapper.OrderDetailMapper;
import com.imooc.gmall.order.mapper.OrderInfoMapper;
import com.imooc.gmall.service.OrderService;
import com.imooc.gmall.service.PaymentSerivce;
import com.imooc.gmall.util.HttpClientUtil;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import javax.jms.Queue;
import java.util.*;


@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private ActiveMQUtil activeMQUtil;
    @Reference
    private PaymentSerivce paymentSerivce;

    @Override
    public String getTradeNo(String userId) {
        Jedis jedis = redisUtil.getJedis();
        String tradeNoKey="user:"+userId+":tradeCode";
        String tradeCode = UUID.randomUUID().toString();
        jedis.setex(tradeNoKey,10*60,tradeCode);
        jedis.close();
        return tradeCode;

    }

    @Override
    @Transactional
    public String saveOrder(OrderInfo orderInfo) {
        // 设置创建时间
        orderInfo.setCreateTime(new Date());
        // 设置失效时间
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE,1);
        orderInfo.setExpireTime(calendar.getTime());
        // 生成第三方支付编号
        String outTradeNo="ATGUIGU"+System.currentTimeMillis()+""+new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradeNo);
        orderInfoMapper.insertSelective(orderInfo);
        // 插入订单详细信息
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            orderDetail.setOrderId(orderInfo.getId());
            orderDetailMapper.insertSelective(orderDetail);
        }
        // 为了跳转到支付页面使用。支付会根据订单id进行支付。
        return orderInfo.getId();
    }
    //验证流水号
    @Override
    public boolean checkTradeCode(String userId, String tradeNo) {
        Jedis jedis = redisUtil.getJedis();
        String tradeNoKey = "user:"+userId+":tradeCode";
        String tradeCode = jedis.get(tradeNoKey);
        jedis.close();
        return tradeCode != null && tradeCode.equals(tradeNo);

    }

    @Override
    public void delTradeNo(String userId) {
        Jedis jedis = redisUtil.getJedis();
        String tradeNoKey =  "user:"+userId+":tradeCode";
        jedis.del(tradeNoKey);
        jedis.close();

    }

    @Override
    public boolean checkStock(String skuId, Integer skuNum) {
        String result = HttpClientUtil.doGet("http://www.gware.com/hasStock?skuId=" + skuId + "&num=" + skuNum);
        return "1".equals(result);

    }

    @Override
    public OrderInfo getOrderInfo(String orderId) {
        // select * from orderInf where id = orderId
        OrderInfo orderInfo = orderInfoMapper.selectByPrimaryKey(orderId);
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setOrderId(orderId);
        orderInfo.setOrderDetailList(orderDetailMapper.select(orderDetail));

        return orderInfo;
    }

    @Override
    public void updateOrderStatus(String orderId, ProcessStatus processStatus) {
        // update orderInfo set processStatus = paid , ordersStatus = paid where id = orderId;
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setProcessStatus(processStatus);
        orderInfo.setId(orderId);
        orderInfo.setOrderStatus(processStatus.getOrderStatus());
        orderInfoMapper.updateByPrimaryKeySelective(orderInfo);

    }
    //生产者 通知库存系统减库存
    @Override
    public void sendOrderStatus(String orderId) {
        // 创建连接
        Connection connection = activeMQUtil.getConnection();
        Session session;
        //构造orderInfo 参考接口文档
        String orderInfoJson = initWareOrder(orderId);
        try {
            connection.start();
            session = connection.createSession(true, Session.SESSION_TRANSACTED);
            // 创建队列
            Queue order_result_queue = session.createQueue("ORDER_RESULT_QUEUE");
            // 创建消息提供者
            MessageProducer producer = session.createProducer(order_result_queue);

            // 创建消息对象
            ActiveMQTextMessage activeMQTextMessage = new ActiveMQTextMessage();
            // orderInfo 组成json 字符串
            activeMQTextMessage.setText(orderInfoJson);

            producer.send(activeMQTextMessage);
            // 提交
            session.commit();

            producer.close();
            session.close();
            connection.close();

        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<OrderInfo> getExpiredOrderList() {
        // 当前系统时间>过期时间 and 当前状态是未支付！
        Example example = new Example(OrderInfo.class);
        example.createCriteria().andEqualTo("processStatus",ProcessStatus.UNPAID).andLessThan("expireTime",new Date());
        return orderInfoMapper.selectByExample(example);
    }

    @Override
    @Async
    public void execExpiredOrder(OrderInfo orderInfo) {
        // 将订单状态改为关闭
        updateOrderStatus(orderInfo.getId(),ProcessStatus.CLOSED);
        // 关闭paymentInfo
        paymentSerivce.closePayment(orderInfo.getId());
    }

    @Override
    public List<OrderInfo> orderSplit(String orderId, String wareSkuMap) {

        List<OrderInfo> subOrderInfoList = new ArrayList<>();
        /*
            1.  获取原始订单
            2.  将wareSkuMap 转换为我们能操作的对象
            3.  创建新的子订单
            4.  给子订单赋值，并保存到数据库
            5.  将子订单添加到集合中
            6.  更新原始订单状态！

         */
        OrderInfo orderInfoOrigin  = getOrderInfo(orderId);
        // wareSkuMap [{"wareId":"1","skuIds":["2","10"]},{"wareId":"2","skuIds":["3"]}]
        List<Map> maps = JSON.parseArray(wareSkuMap,Map.class);
        if (maps!=null){
            // 循环遍历集合
            for (Map map : maps) {
                // 获取仓库Id
                String wareId = (String) map.get("wareId");
                // 获取商品Id
                List<String> skuIds = (List<String>) map.get("skuIds");
                OrderInfo subOrderInfo  = new OrderInfo();
                // 属性拷贝
                BeanUtils.copyProperties(orderInfoOrigin,subOrderInfo);
                // id 必须变为null
                subOrderInfo.setId(null);
                subOrderInfo.setWareId(wareId);
                subOrderInfo.setParentOrderId(orderId);

                // 价格： 获取到原始订单的明细
                List<OrderDetail> orderDetailList = orderInfoOrigin.getOrderDetailList();

                // 声明一个新的子订单明细集合
                List<OrderDetail> subOrderDetailArrayList = new ArrayList<>();
                // 原始的订单明细商品Id
                for (OrderDetail orderDetail : orderDetailList) {
                    // 仓库对应的商品Id
                    for (String skuId : skuIds) {
                        if (skuId.equals(orderDetail.getSkuId())){
                            orderDetail.setId(null);
                            subOrderDetailArrayList.add(orderDetail);
                        }
                    }
                }
                // 将新的子订单集合放入子订单中
                subOrderInfo.setOrderDetailList(subOrderDetailArrayList);

                // 计算价格：
                subOrderInfo.sumTotalAmount();

                // 保存到数据库
                saveOrder(subOrderInfo);

                // 将新的子订单添加到集合中
                subOrderInfoList.add(subOrderInfo);
            }
        }
        updateOrderStatus(orderId,ProcessStatus.SPLIT);
        return subOrderInfoList;
    }

    /**
     *  根据orderId 将orderInfo 变为json 字符串
     * @param orderId
     * @return
     */
    private String initWareOrder(String orderId) {
        // 根据orderId 查询orderInfo
        OrderInfo orderInfo = getOrderInfo(orderId);
        // 将orderInfo 中有用的信息保存到map 中！
        Map<String, Object> map = initWareOrder(orderInfo);
        // 将map 转换为json  字符串！
        return JSON.toJSONString(map);

    }

    /**
     *
     * @param orderInfo
     * @return
     */
    public Map<String, Object> initWareOrder(OrderInfo orderInfo) {
        HashMap<String, Object> map = new HashMap<>();
        // 给map 的key 赋值！
        map.put("orderId",orderInfo.getId());
        map.put("consignee", orderInfo.getConsignee());
        map.put("consigneeTel",orderInfo.getConsigneeTel());
        map.put("orderComment",orderInfo.getOrderComment());
        map.put("orderBody","测试用例");
        map.put("deliveryAddress",orderInfo.getDeliveryAddress());
        map.put("paymentWay","2");

        map.put("wareId",orderInfo.getWareId()); // 仓库Id
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();

        // 创建一个集合来存储map
        List<Map<String, Object>> arrayList = new ArrayList<>();
        for (OrderDetail orderDetail : orderDetailList) {
            HashMap<String, Object> orderDetailMap = new HashMap<>();
            orderDetailMap.put("skuId",orderDetail.getSkuId());
            orderDetailMap.put("skuNum",orderDetail.getSkuNum());
            orderDetailMap.put("skuName",orderDetail.getSkuName());
            arrayList.add(orderDetailMap);
        }
        map.put("details",arrayList);

        return map;
    }
}
