package com.im.application.netty.processor;
import com.im.common.domain.enums.SendMessageType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 消息处理器工厂
 * 用于获取不同类型消息的处理器
 * 
 * 支持的消息类型：
 * - LOGIN: 登录消息
 * - HEARTBEAT: 心跳消息
 * - PRIVATE_CHAT: 单聊消息
 * - GROUP_CHAT: 群聊消息
 */
@Slf4j
@Component
public class ProcessorFactory {
    
    @Autowired
    private LoginProcessor loginProcessor;
    
    @Autowired
    private HeartBeatProcessor heartBeatProcessor;
    
    // TODO: 注入其他消息处理器
    // @Autowired
    // private PrivateChatProcessor privateChatProcessor;
    //
    // @Autowired
    // private GroupChatProcessor groupChatProcessor;
    
    /**
     * 根据消息类型获取对应的消息处理器
     * 
     * @param messageType 消息类型
     * @return 消息处理器
     */
    public MessageProcessor<?> getProcessor(SendMessageType messageType) {
        log.debug("获取消息处理器: messageType={}", messageType);
        
        if (messageType == null) {
            log.warn("消息类型为空");
            return null;
        }
        
        switch (messageType) {
            case LOGIN:
                log.debug("返回登录消息处理器");
                return loginProcessor;
            
            case HEARTBEAT:
                log.debug("返回心跳消息处理器");
                return heartBeatProcessor;
            
            case PRIVATE_CHAT:
                log.debug("返回单聊消息处理器");
                // TODO: 实现单聊消息处理器
                // return privateChatProcessor;
                return null;
            
            case GROUP_CHAT:
                log.debug("返回群聊消息处理器");
                // TODO: 实现群聊消息处理器
                // return groupChatProcessor;
                return null;
            
            case FORCE_LOGOUT:
                log.debug("强制下线消息");
                // 强制下线消息由系统内部处理，不需要处理器
                return null;
            
            default:
                log.warn("未知的消息类型: {}", messageType);
                return null;
        }
    }
    
    /**
     * 根据消息类型字符串获取对应的消息处理器
     * 
     * @param messageTypeStr 消息类型字符串
     * @return 消息处理器
     */
    public MessageProcessor<?> getProcessor(String messageTypeStr) {
        SendMessageType messageType = convertToSendMessageType(messageTypeStr);
        return getProcessor(messageType);
    }
    
    /**
     * 从消息内容中提取消息类型
     * 
     * @param message 消息内容（JSON格式）
     * @return 消息类型
     */
    public SendMessageType extractMessageType(String message) {
        try {
            if (message == null || message.isEmpty()) {
                return null;
            }
            
            // 简单的JSON解析，提取type字段
            // 实际项目中应使用JSON库（如Jackson、Gson等）
            int typeStart = message.indexOf("\"type\"");
            if (typeStart == -1) {
                return null;
            }
            
            typeStart = message.indexOf("\"", typeStart + 7) + 1;
            int typeEnd = message.indexOf("\"", typeStart);
            
            if (typeStart > 0 && typeEnd > typeStart) {
                String typeStr = message.substring(typeStart, typeEnd);
                return convertToSendMessageType(typeStr);
            }
            
            return null;
        } catch (Exception e) {
            log.error("提取消息类型异常: message={}", message, e);
            return null;
        }
    }
    
    /**
     * 将字符串转换为 SendMessageType 枚举
     * 
     * @param typeStr 消息类型字符串
     * @return SendMessageType 枚举值，如果无法转换则返回 null
     */
    private SendMessageType convertToSendMessageType(String typeStr) {
        if (typeStr == null || typeStr.isEmpty()) {
            return null;
        }
        
        try {
            // 尝试按照枚举名称匹配（大小写不敏感）
            for (SendMessageType type : SendMessageType.values()) {
                if (type.name().equalsIgnoreCase(typeStr)) {
                    return type;
                }
            }
            
            // 尝试按照枚举描述匹配
            for (SendMessageType type : SendMessageType.values()) {
                if (type.getDesc().equalsIgnoreCase(typeStr)) {
                    return type;
                }
            }
            
            log.warn("无法转换消息类型字符串: {}", typeStr);
            return null;
        } catch (Exception e) {
            log.error("转换消息类型异常: typeStr={}", typeStr, e);
            return null;
        }
    }
}
