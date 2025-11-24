package com.im.application.consumer;

import com.im.common.domain.model.CommonReceiveData;
import com.im.common.domain.model.PrivateChatData;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQPushConsumerLifecycleListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 私聊消息消费者
 * 继承BaseMessageConsumer，实现RocketMQListener<String>和RocketMQPushConsumerLifecycleListener接口
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = "", // 占位符，实际topic会在prepareStart方法中动态添加
        consumerGroup = "IM_PRIVATE_MESSAGE_CONSUMER_GROUP",
        selectorExpression = "*"
)
public class PrivateMessageConsumer extends BaseMessageConsumer implements RocketMQListener<String>, RocketMQPushConsumerLifecycleListener {
    
    /**
     * 服务ID，用于动态订阅与该服务相关的Topic
     */
    @Value("${server.id}")
    private String serverId;
    
    /**
     * 主题前缀
     */
    private static final String TOPIC_PREFIX = "IM_PRIVATE_MESSAGE_TOPIC_";
    
    /**
     * 消费组前缀
     */
    private static final String CONSUMER_GROUP_PREFIX = "IM_PRIVATE_MESSAGE_CONSUMER_GROUP_";
    
    /**
     * 标签
     */
    private static final String TAG = "*";
    
    /**
     * 实现RocketMQListener接口的onMessage方法
     * 处理接收到的消息
     */
    @Override
    public void onMessage(String message) {
        try {
            // 使用父类的方法将String消息转换为CommonReceiveData
            CommonReceiveData<PrivateChatData> receiveData = getReceiveMessage(message, PrivateChatData.class);
            
            if (receiveData == null) {
                log.error("消息转换失败，跳过处理");
                return;
            }
            
            // 获取具体的消息数据
            PrivateChatData chatData = receiveData.getData();
            if (chatData == null) {
                log.error("消息数据为空，跳过处理: receiveData={}", receiveData);
                return;
            }
            
            log.info("开始处理私聊消息: senderId={}, receiverId={}", 
                    chatData.getSender() != null ? chatData.getSender().getUserId() : "null",
                    chatData.getReceiverId());
            
            // TODO: 实现具体的消息处理逻辑
            // 1. 检查接收者是否在线
            // 2. 如果在线，发送消息；如果不在线，存储离线消息
            // 3. 处理发送到自己其他终端的逻辑
            // 4. 处理回推发送结果的逻辑
            
            log.info("私聊消息处理完成");
            
        } catch (Exception e) {
            log.error("处理私聊消息异常: message={}", message, e);
        }
    }
    
    /**
     * 实现RocketMQPushConsumerLifecycleListener接口的prepareStart方法
     * 用于动态设置消费者的订阅主题和其他配置
     */
    @Override
    public void prepareStart(DefaultMQPushConsumer consumer) {
        try {
            // 1. 构造动态主题
            String dynamicTopic = TOPIC_PREFIX + serverId;
            
            // 2. 设置消费组
            String consumerGroup = CONSUMER_GROUP_PREFIX + serverId;
            consumer.setConsumerGroup(consumerGroup);
            
            // 3. 订阅主题和标签
            consumer.subscribe(dynamicTopic, TAG);
            
            // 4. 设置消费者的其他配置
            consumer.setConsumeFromWhere("CONSUME_FROM_LAST_OFFSET");
            
            // 5. 记录订阅信息
            log.info("私聊消息消费者订阅配置 - 主题: {}, 标签: {}, 消费组: {}", 
                    dynamicTopic, TAG, consumerGroup);
            
        } catch (Exception e) {
            log.error("设置私聊消息消费者配置异常", e);
            throw new RuntimeException("设置私聊消息消费者配置失败", e);
        }
    }
}
