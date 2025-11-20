package com.im.application.netty.processor.factory;
import com.im.application.netty.processor.impl.HeartBeatProcessor;
import com.im.application.netty.processor.impl.LoginProcessor;
import com.im.application.netty.processor.MessageProcessor;
import com.im.common.domain.enums.SendMessageType;
import com.im.common.domain.model.CommonSendData;
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
//        log.debug("获取消息处理器: messageType={}", messageType);
        
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
}
