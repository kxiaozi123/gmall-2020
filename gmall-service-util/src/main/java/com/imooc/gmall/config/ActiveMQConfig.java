package com.imooc.gmall.config;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.jboss.netty.util.internal.ReusableIterator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;

import javax.jms.Session;

@Configuration
public class ActiveMQConfig {

    @Value("${spring.activemq.broker-url:disabled}")
    String brokerURL ;

    @Value("${activemq.listener.enable:disabled}")
    String listenerEnable;

    // 在spring 容器中添加一个ActiveMQUtil 的实例
    @Bean
    public ActiveMQUtil getActiveMQUtil(){
        if ("disabled".equals(brokerURL)){
            return null;
        }
        ActiveMQUtil activeMQUtil = new ActiveMQUtil();
        activeMQUtil.init(brokerURL);
        return  activeMQUtil;
    }
    /*
        <beans>
            <bean class="com.atguigu.gmall0218.config.ActiveMQUtil">

            <bean/>

            <bean id="activeMQConnectionFactory" class="org.apache.activemq.ActiveMQConnectionFactory">
                <property name="brokerURL" value="tcp://192.168.67.219:61616">
            </bean>

            <bean class="org.springframework.jms.config.DefaultJmsListenerContainerFactory">
                <property name = "activeMQConnectionFactory" ref="activeMQConnectionFactory">
            </bean>
        </beans>

     */
    // 专门用来配置消息监听器工厂！
    @Bean(name = "jmsQueueListener")
    public DefaultJmsListenerContainerFactory jmsQueueListenerContainerFactory(ActiveMQConnectionFactory activeMQConnectionFactory) {

        if("disabled".equals(listenerEnable)){
            return null;
        }
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(activeMQConnectionFactory);
        // 设置事务
        factory.setSessionTransacted(false);
        // 自动签收
        factory.setSessionAcknowledgeMode(Session.AUTO_ACKNOWLEDGE);
        // 设置并发数
        factory.setConcurrency("5");
        // 重连间隔时间
        factory.setRecoveryInterval(5000L);

        return factory;
    }
    // 接收消息的工厂
    // ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory("tcp://192.168.67.219:61616");
    @Bean
    public ActiveMQConnectionFactory activeMQConnectionFactory ( ){
        ActiveMQConnectionFactory activeMQConnectionFactory =
                new ActiveMQConnectionFactory(brokerURL);
        return activeMQConnectionFactory;
    }
}
