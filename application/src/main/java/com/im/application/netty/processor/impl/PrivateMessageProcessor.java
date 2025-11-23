package com.im.application.netty.processor.impl;

import com.alibaba.fastjson.JSON;
import com.im.application.netty.cache.UserChannelContextCache;
import com.im.application.netty.processor.MessageProcessor;
import com.im.common.domain.enums.TerminalType;
import com.im.common.domain.model.PrivateChatData;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 私聊消息处理器
 * 实现MessageProcessor接口，处理私聊消息的逻辑
 */
@Slf4j
@Component
public class PrivateMessageProcessor implements MessageProcessor<PrivateChatData> {
    
    @Autowired
    private UserChannelContextCache userChannelContextCache;
    
    /**
     * 处理消息（不带通道上下文）
     * 只实现必要的process方法，其他方法使用默认实现
     * 
     * @param data 私聊消息数据
     */
    @Override
    public void process(PrivateChatData data) {
        try {
            // 1. 验证消息数据不为空
            if (data == null) {
                log.error("私聊消息数据为空");
                return;
            }
            
            // 2. 获取发送者信息和接收者ID
            String senderId = data.getSender() != null ? data.getSender().getUserId() : null;
            String receiverId = data.getReceiverId();
            List<TerminalType> receiverTerminals = data.getReceiverTerminals();
            
            if (senderId == null || receiverId == null) {
                log.error("发送者ID或接收者ID为空: senderId={}, receiverId={}", senderId, receiverId);
                return;
            }
            
            log.info("私聊消息: senderId={}, receiverId={}, receiverTerminals={}", 
                    senderId, receiverId, receiverTerminals);
            
            // 3. 处理接收者终端列表
            if (receiverTerminals != null && !receiverTerminals.isEmpty()) {
                // 向指定终端发送消息
                for (TerminalType terminalType : receiverTerminals) {
                    sendToReceiver(receiverId, terminalType, data);
                }
            } else {
                // 如果没有指定终端，向所有在线终端发送消息
                log.info("未指定接收者终端，将向所有在线终端发送消息");
                // TODO: 实现向所有在线终端发送消息的逻辑
            }
            
            // 4. 处理发送到自己其他终端的逻辑
            if (data.isSendToSelfOtherTerminals()) {
                log.info("需要发送到自己其他终端");
                // TODO: 实现发送到自己其他终端的逻辑
            }
            
            // 5. 处理回推发送结果的逻辑
            if (data.isPushSendResult()) {
                log.info("需要回推发送结果");
                // TODO: 实现回推发送结果的逻辑
            }
            
        } catch (Exception e) {
            log.error("处理私聊消息异常: data={}", data, e);
        }
    }
    
    /**
     * 向指定接收者的指定终端发送消息
     * 
     * @param receiverId 接收者ID
     * @param terminalType 终端类型
     * @param data 消息数据
     */
    private void sendToReceiver(String receiverId, TerminalType terminalType, PrivateChatData data) {
        try {
            // 尝试获取接收者的连接信息
            ChannelHandlerContext receiverCtx = userChannelContextCache.get(Long.valueOf(receiverId), terminalType.getCode());
            
            if (receiverCtx != null && receiverCtx.channel().isActive()) {
                // 接收者在线，封装消息并发送
                log.info("接收者在线，发送消息: receiverId={}, terminalType={}", receiverId, terminalType);
                
                // 将消息转换为JSON字符串发送
                String messageJson = JSON.toJSONString(data);
                receiverCtx.writeAndFlush(messageJson);
                
                log.info("消息发送成功: receiverId={}, terminalType={}", receiverId, terminalType);
                
            } else {
                // 接收者不在线或找不到对应的终端类型，向消息中间件发送未找到的消息
                log.warn("未找到接收者的连接信息或接收者不在线: receiverId={}, terminalType={}", 
                        receiverId, terminalType);
                
                // TODO: 实现向消息中间件发送未找到消息的逻辑
            }
            
        } catch (Exception e) {
            log.error("向接收者发送消息异常: receiverId={}, terminalType={}", receiverId, terminalType, e);
        }
    }
}