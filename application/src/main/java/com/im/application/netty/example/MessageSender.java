package com.im.application.netty.example;
import com.im.application.netty.cache.UserChannelContextCache;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 消息发送器示例
 * 展示如何使用UserChannelContextCache发送消息给用户
 */
@Slf4j
@Component
public class MessageSender {
    
    @Autowired
    private UserChannelContextCache channelCache;
    
    /**
     * 发送消息给指定用户的指定终端
     * 
     * @param userId 用户ID
     * @param terminalType 终端类型
     * @param message 消息内容
     * @return 是否发送成功
     */
    public boolean sendToUser(Long userId, Integer terminalType, String message) {
        ChannelHandlerContext ctx = channelCache.get(userId, terminalType);
        
        if (ctx == null) {
            log.warn("用户不在线或连接不存在: userId={}, terminalType={}", userId, terminalType);
            return false;
        }
        
        if (!ctx.channel().isActive()) {
            log.warn("用户连接已断开: userId={}, terminalType={}", userId, terminalType);
            channelCache.remove(userId, terminalType);
            return false;
        }
        
        try {
            // 将消息转换为ByteBuf
            ByteBuf buffer = Unpooled.copiedBuffer(message, StandardCharsets.UTF_8);
            ctx.writeAndFlush(buffer);
            log.info("消息发送成功: userId={}, terminalType={}, message={}", userId, terminalType, message);
            return true;
        } catch (Exception e) {
            log.error("消息发送失败: userId={}, terminalType={}", userId, terminalType, e);
            return false;
        }
    }
    
    /**
     * 发送消息给用户的所有终端
     * 
     * @param userId 用户ID
     * @param message 消息内容
     * @return 成功发送的终端数量
     */
    public int sendToAllTerminals(Long userId, String message) {
        Map<Integer, ChannelHandlerContext> terminals = channelCache.getAll(userId);
        
        if (terminals == null || terminals.isEmpty()) {
            log.warn("用户没有在线终端: userId={}", userId);
            return 0;
        }
        
        int successCount = 0;
        for (Map.Entry<Integer, ChannelHandlerContext> entry : terminals.entrySet()) {
            Integer terminalType = entry.getKey();
            ChannelHandlerContext ctx = entry.getValue();
            
            if (ctx.channel().isActive()) {
                try {
                    ByteBuf buffer = Unpooled.copiedBuffer(message, StandardCharsets.UTF_8);
                    ctx.writeAndFlush(buffer);
                    successCount++;
                    log.info("消息发送成功: userId={}, terminalType={}", userId, terminalType);
                } catch (Exception e) {
                    log.error("消息发送失败: userId={}, terminalType={}", userId, terminalType, e);
                }
            } else {
                log.warn("终端连接已断开: userId={}, terminalType={}", userId, terminalType);
                channelCache.remove(userId, terminalType);
            }
        }
        
        log.info("消息发送完成: userId={}, 成功数={}, 总数={}", userId, successCount, terminals.size());
        return successCount;
    }
    
    /**
     * 广播消息给所有在线用户
     * 
     * @param message 消息内容
     * @return 成功发送的连接数量
     */
    public int broadcast(String message) {
        int totalConnections = channelCache.getTotalConnectionCount();
        log.info("开始广播消息，当前在线连接数: {}", totalConnections);
        
        // 注意：这里只是示例，实际应该遍历所有用户
        // 由于UserChannelContextCache没有提供获取所有用户的方法，
        // 实际使用时需要维护一个用户ID列表或者扩展UserChannelContextCache
        
        log.warn("广播功能需要扩展UserChannelContextCache以支持获取所有用户ID");
        return 0;
    }
    
    /**
     * 检查用户是否在线
     * 
     * @param userId 用户ID
     * @param terminalType 终端类型
     * @return 是否在线
     */
    public boolean isUserOnline(Long userId, Integer terminalType) {
        return channelCache.isOnline(userId, terminalType);
    }
    
    /**
     * 获取在线统计信息
     * 
     * @return 统计信息字符串
     */
    public String getOnlineStats() {
        int userCount = channelCache.getOnlineUserCount();
        int connectionCount = channelCache.getTotalConnectionCount();
        
        return String.format("在线用户数: %d, 总连接数: %d", userCount, connectionCount);
    }
}
