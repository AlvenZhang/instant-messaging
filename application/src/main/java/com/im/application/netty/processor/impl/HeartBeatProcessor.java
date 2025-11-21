package com.im.application.netty.processor.impl;
import com.im.application.netty.cache.UserChannelContextCache;
import com.im.application.netty.processor.MessageProcessor;
import com.im.common.cache.distribute.DistributedCache;
import com.im.common.domain.constant.IMConstants;
import com.im.infrastructure.holder.SpringContextHolder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 心跳消息处理器
 * 实现MessageProcessor接口，主要处理心跳消息逻辑
 * 
 * 处理流程：
 * 1. 响应客户端的心跳消息
 * 2. 从Channel链接中获取当前心跳次数
 * 3. 对心跳次数+1重新放入Channel链接中
 * 4. 对当前心跳次数求模，结果为0则延长分布式缓存中的有效时长
 */
@Slf4j
@Component
public class HeartBeatProcessor implements MessageProcessor<String> {
    
    @Autowired
    private UserChannelContextCache userChannelContextCache;
    
    /**
     * 心跳次数阈值
     * 每隔这个次数的心跳，就延长一次分布式缓存中的有效时长
     */
    @Value("${im.heartbeat.threshold:10}")
    private Integer heartbeatThreshold;
    
    /**
     * 分布式缓存过期时间（秒）
     * 用于延长用户与服务ID的映射关系的有效时长
     */
    @Value("${im.heartbeat.cache-expire-time:86400}")
    private Integer cacheExpireTime;
    
    /**
     * 处理心跳消息（带通道上下文）
     * 
     * @param ctx 通道处理器上下文
     * @param data 心跳消息数据
     */
    @Override
    public void process(ChannelHandlerContext ctx, String data) {
        log.debug("处理心跳消息: channelId={}, data={}", ctx.channel().id().asShortText(), data);
        
        try {
            // 1. 响应客户端的心跳消息
            sendHeartbeatResponse(ctx);
            
            // 2. 从Channel链接中获取用户ID和终端类型
            AttributeKey<Long> userIdAttr = AttributeKey.valueOf(IMConstants.USER_ID);
            Long userId = ctx.channel().attr(userIdAttr).get();
            AttributeKey<Integer> terminalAttr = AttributeKey.valueOf(IMConstants.TERMINAL_TYPE);
            Integer terminalType = ctx.channel().attr(terminalAttr).get();
            
            if (userId == null || terminalType == null) {
                log.warn("无法从Channel中获取用户信息: channelId={}", ctx.channel().id().asShortText());
                return;
            }
            
            // 3. 获取当前心跳次数
            AttributeKey<Integer> heartbeatCountAttr = AttributeKey.valueOf("HEARTBEAT_COUNT");
            Integer currentHeartbeatCount = ctx.channel().attr(heartbeatCountAttr).get();
            
            if (currentHeartbeatCount == null) {
                currentHeartbeatCount = 0;
            }
            
            // 4. 心跳次数+1
            int newHeartbeatCount = currentHeartbeatCount + 1;
            ctx.channel().attr(heartbeatCountAttr).set(newHeartbeatCount);
            
            log.debug("更新心跳次数: userId={}, terminalType={}, heartbeatCount={}", 
                    userId, terminalType, newHeartbeatCount);
            
            // 5. 对心跳次数求模，结果为0则延长分布式缓存中的有效时长
            if (newHeartbeatCount % heartbeatThreshold == 0) {
                log.debug("心跳次数达到阈值，延长分布式缓存有效时长: userId={}, terminalType={}, heartbeatCount={}", 
                        userId, terminalType, newHeartbeatCount);
                
                refreshServerIdCache(userId, terminalType);
            }
            
        } catch (Exception e) {
            log.error("处理心跳消息异常: channelId={}", ctx.channel().id().asShortText(), e);
        }
    }
    
    /**
     * 处理心跳消息（不带通道上下文）
     * 
     * @param data 心跳消息数据
     */
    @Override
    public void process(String data) {
        log.warn("调用了不带通道上下文的process方法，心跳消息需要通道上下文");
    }
    
    /**
     * 将对象转换为String类型
     * 
     * @param obj 原始对象
     * @return 转换后的String
     */
    @Override
    public String transform(Object obj) {
        if (obj == null) {
            return null;
        }
        return obj.toString();
    }
    
    /**
     * 发送心跳响应
     * 
     * @param ctx 通道处理器上下文
     */
    private void sendHeartbeatResponse(ChannelHandlerContext ctx) {
        try {
            String response = "{\"type\":\"heartbeat_response\",\"timestamp\":" + System.currentTimeMillis() + "}";
            ctx.writeAndFlush(response);
            log.debug("发送心跳响应: channelId={}", ctx.channel().id().asShortText());
        } catch (Exception e) {
            log.error("发送心跳响应异常: channelId={}", ctx.channel().id().asShortText(), e);
        }
    }
    
    /**
     * 刷新分布式缓存中的服务ID有效时长
     * 
     * @param userId 用户ID
     * @param terminalType 终端类型
     */
    private void refreshServerIdCache(Long userId, Integer terminalType) {
        try {
            DistributedCache distributedCache = SpringContextHolder.getBean(IMConstants.DISTRIBUTED_CACHE_REDIS_SERVICE_KEY);
            
            // 构建Redis Key
            String redisKey = String.join(IMConstants.REDIS_KEY_SPLIT, 
                    IMConstants.IM_USER_SERVER_ID, userId.toString(), terminalType.toString());
            
            // 获取当前的服务ID
            Long serverId = distributedCache.get(redisKey, Long.class);
            
            if (serverId != null) {
                // 重新设置缓存，延长有效时长
//                distributedCache.set(redisKey, serverId, Duration.ofSeconds(cacheExpireTime));
                distributedCache.expire(redisKey, Duration.ofSeconds(cacheExpireTime));
                
                log.info("刷新分布式缓存成功: userId={}, terminalType={}, serverId={}, expireTime={}", 
                        userId, terminalType, serverId, cacheExpireTime);
            } else {
                log.warn("分布式缓存中不存在服务ID: userId={}, terminalType={}", userId, terminalType);
            }
        } catch (Exception e) {
            log.error("刷新分布式缓存异常: userId={}, terminalType={}", userId, terminalType, e);
        }
    }
}
