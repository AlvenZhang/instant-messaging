package com.im.application.netty.cache;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户连接上下文缓存
 * 用于存储用户ID、终端类型与ChannelHandlerContext的映射关系
 */
@Slf4j
@Component
public class UserChannelContextCache {
    
    /**
     * 用户连接缓存
     * 外层Map的Key是用户ID
     * 内层Map的Key是用户终端类型，Value是ChannelHandlerContext对象
     */
    private final Map<Long, Map<Integer, ChannelHandlerContext>> userChannelMap = new ConcurrentHashMap<>();
    
    /**
     * 存储用户连接
     * @param userId 用户ID
     * @param terminalType 终端类型
     * @param ctx ChannelHandlerContext对象
     */
    public void put(Long userId, Integer terminalType, ChannelHandlerContext ctx) {
        if (userId == null || terminalType == null || ctx == null) {
            log.warn("存储用户连接失败，参数不能为空: userId={}, terminalType={}, ctx={}", userId, terminalType, ctx);
            return;
        }
        
        userChannelMap.computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
                      .put(terminalType, ctx);
        log.info("存储用户连接成功: userId={}, terminalType={}", userId, terminalType);
    }
    
    /**
     * 移除用户连接
     * @param userId 用户ID
     * @param terminalType 终端类型
     */
    public void remove(Long userId, Integer terminalType) {
        if (userId == null || terminalType == null) {
            log.warn("移除用户连接失败，参数不能为空: userId={}, terminalType={}", userId, terminalType);
            return;
        }
        
        Map<Integer, ChannelHandlerContext> terminalMap = userChannelMap.get(userId);
        if (terminalMap != null) {
            terminalMap.remove(terminalType);
            log.info("移除用户连接成功: userId={}, terminalType={}", userId, terminalType);
            
            // 如果该用户没有任何终端连接了，则移除整个用户的记录
            if (terminalMap.isEmpty()) {
                userChannelMap.remove(userId);
                log.info("用户所有终端连接已移除: userId={}", userId);
            }
        }
    }
    
    /**
     * 移除用户的所有连接
     * @param userId 用户ID
     */
    public void removeAll(Long userId) {
        if (userId == null) {
            log.warn("移除用户所有连接失败，用户ID不能为空");
            return;
        }
        
        Map<Integer, ChannelHandlerContext> removed = userChannelMap.remove(userId);
        if (removed != null) {
            log.info("移除用户所有连接成功: userId={}, 终端数量={}", userId, removed.size());
        }
    }
    
    /**
     * 获取用户指定终端的连接
     * @param userId 用户ID
     * @param terminalType 终端类型
     * @return ChannelHandlerContext对象，如果不存在则返回null
     */
    public ChannelHandlerContext get(Long userId, Integer terminalType) {
        if (userId == null || terminalType == null) {
            log.warn("获取用户连接失败，参数不能为空: userId={}, terminalType={}", userId, terminalType);
            return null;
        }
        
        Map<Integer, ChannelHandlerContext> terminalMap = userChannelMap.get(userId);
        if (terminalMap != null) {
            return terminalMap.get(terminalType);
        }
        return null;
    }
    
    /**
     * 获取用户所有终端的连接
     * @param userId 用户ID
     * @return 用户所有终端的连接Map，如果不存在则返回null
     */
    public Map<Integer, ChannelHandlerContext> getAll(Long userId) {
        if (userId == null) {
            log.warn("获取用户所有连接失败，用户ID不能为空");
            return null;
        }
        
        return userChannelMap.get(userId);
    }
    
    /**
     * 判断用户指定终端是否在线
     * @param userId 用户ID
     * @param terminalType 终端类型
     * @return true表示在线，false表示不在线
     */
    public boolean isOnline(Long userId, Integer terminalType) {
        ChannelHandlerContext ctx = get(userId, terminalType);
        return ctx != null && ctx.channel().isActive();
    }
    
    /**
     * 获取在线用户数量
     * @return 在线用户数量
     */
    public int getOnlineUserCount() {
        return userChannelMap.size();
    }
    
    /**
     * 获取所有在线连接数量
     * @return 所有在线连接数量
     */
    public int getTotalConnectionCount() {
        return userChannelMap.values().stream()
                .mapToInt(Map::size)
                .sum();
    }
}
