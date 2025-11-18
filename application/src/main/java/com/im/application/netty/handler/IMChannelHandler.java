package com.im.application.netty.handler;
import com.im.application.netty.cache.UserChannelContextCache;
import com.im.common.cache.distribute.DistributedCache;
import com.im.infrastructure.holder.SpringContextHolder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * IM即时通讯消息处理器
 * 继承SimpleChannelInboundHandler处理入站事件和数据
 */
@Slf4j
@Component
public class IMChannelHandler extends SimpleChannelInboundHandler<String> {
    
    /**
     * 通道属性Key：用户ID
     */
    public static final AttributeKey<Long> USER_ID = AttributeKey.valueOf("USER_ID");
    
    /**
     * 通道属性Key：终端类型
     */
    public static final AttributeKey<Integer> TERMINAL_TYPE = AttributeKey.valueOf("TERMINAL_TYPE");
    
    @Autowired
    private UserChannelContextCache userChannelContextCache;
    
    /**
     * 处理入站事件和数据
     * 当接收到客户端发送的消息时，会调用此方法
     * 
     * @param ctx 通道处理器上下文
     * @param msg 接收到的消息
     * @throws Exception 处理过程中可能抛出的异常
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        log.info("接收到消息: channelId={}, message={}", ctx.channel().id().asShortText(), msg);
        
        // 从通道属性中获取用户ID和终端类型
        Long userId = ctx.channel().attr(USER_ID).get();
        Integer terminalType = ctx.channel().attr(TERMINAL_TYPE).get();
        
        if (userId != null && terminalType != null) {
            log.info("处理用户消息: userId={}, terminalType={}, message={}", userId, terminalType, msg);
            // TODO: 在这里添加具体的业务逻辑处理
            // 例如：消息路由、消息存储、消息转发等
        } else {
            log.warn("通道属性中未找到用户信息: channelId={}", ctx.channel().id().asShortText());
        }
    }
    
    /**
     * 处理异常
     * 当处理过程中发生异常时，会调用此方法
     * 
     * @param ctx 通道处理器上下文
     * @param cause 异常原因
     * @throws Exception 处理过程中可能抛出的异常
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("通道异常: channelId={}, error={}", 
                ctx.channel().id().asShortText(), cause.getMessage(), cause);
        
        // 从通道属性中获取用户信息用于日志记录
        Long userId = ctx.channel().attr(USER_ID).get();
        Integer terminalType = ctx.channel().attr(TERMINAL_TYPE).get();
        
        if (userId != null && terminalType != null) {
            log.error("用户连接异常: userId={}, terminalType={}", userId, terminalType);
        }
        
        // 关闭连接
        ctx.close();
    }
    
    /**
     * 用户终端与即时通讯后端服务建立连接后Netty回调的方法
     * 可以执行一些保存操作，但此时在这个方法里没有做任何操作
     * 
     * @param ctx 通道处理器上下文
     * @throws Exception 处理过程中可能抛出的异常
     */
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        log.info("新连接建立: channelId={}", ctx.channel().id().asShortText());
        // 此时用户还未登录，通道属性中还没有用户信息
        // 用户登录后会设置通道属性，并将连接信息存储到缓存中
    }
    
    /**
     * 用户终端与即时通讯后端服务断开连接后Netty回调的方法
     * 从通道属性中获取到用户ID和终端类型，通过用户ID和终端类型将其从本地缓存和分布式缓存中删除
     * 通过判断缓存中存在该用户的通道上下文、缓存中的通道ID与当前断开连接的通道ID一致来防止异地登录误删连接
     * 
     * @param ctx 通道处理器上下文
     * @throws Exception 处理过程中可能抛出的异常
     */
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        log.info("连接断开: channelId={}", ctx.channel().id().asShortText());
        
        // 从通道属性中获取用户ID和终端类型
        Long userId = ctx.channel().attr(USER_ID).get();
        Integer terminalType = ctx.channel().attr(TERMINAL_TYPE).get();
        
        if (userId == null || terminalType == null) {
            log.info("连接断开，但通道属性中未找到用户信息: channelId={}", ctx.channel().id().asShortText());
            return;
        }
        
        log.info("用户连接断开: userId={}, terminalType={}, channelId={}", 
                userId, terminalType, ctx.channel().id().asShortText());
        
        // 从本地缓存中获取该用户的连接上下文
        ChannelHandlerContext cachedCtx = userChannelContextCache.get(userId, terminalType);
        
        // 防止异地登录误删连接：
        // 1. 缓存中存在该用户的通道上下文
        // 2. 缓存中的通道ID与当前断开连接的通道ID一致
        if (cachedCtx != null && cachedCtx.channel().id().equals(ctx.channel().id())) {
            // 从本地缓存中移除用户连接
            userChannelContextCache.remove(userId, terminalType);
            log.info("从本地缓存中移除用户连接: userId={}, terminalType={}", userId, terminalType);
            
            // TODO: 从分布式缓存中删除用户连接信息
            DistributedCache distributedCacheService = SpringContextHolder.getBean(IMConstants.DISTRIBUTED_CACHE_REDIS_SERVICE_KEY);
            String redisKey = String.join(IMConstants.REDIS_KEY_SPLIT, IMConstants.IM_USER_SERVER_ID, userId.toString(), terminal.toString());
            distributedCacheService.delete(redisKey);
            log.info("从分布式缓存中移除用户连接: userId={}, terminalType={}", userId, terminalType);
        } else {
            if (cachedCtx == null) {
                log.warn("缓存中不存在该用户的连接，可能已被移除: userId={}, terminalType={}", userId, terminalType);
            } else {
                log.warn("缓存中的通道ID与当前断开连接的通道ID不一致，可能是异地登录: userId={}, terminalType={}, cachedChannelId={}, currentChannelId={}", 
                        userId, terminalType, cachedCtx.channel().id().asShortText(), ctx.channel().id().asShortText());
            }
        }
    }
    
    /**
     * 用户事件触发
     * 在userEventTriggered()方法中检测超时时间，读超时事件中关闭对应的Channel连接
     * 
     * @param ctx 通道处理器上下文
     * @param evt 用户事件
     * @throws Exception 处理过程中可能抛出的异常
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            
            // 检测读超时事件
            if (event.state() == IdleState.READER_IDLE) {
                log.warn("读超时，关闭连接: channelId={}", ctx.channel().id().asShortText());
                
                // 从通道属性中获取用户信息用于日志记录
                Long userId = ctx.channel().attr(USER_ID).get();
                Integer terminalType = ctx.channel().attr(TERMINAL_TYPE).get();
                
                if (userId != null && terminalType != null) {
                    log.warn("用户连接读超时: userId={}, terminalType={}", userId, terminalType);
                }
                
                // 关闭连接
                ctx.close();
            } else if (event.state() == IdleState.WRITER_IDLE) {
                log.debug("写超时: channelId={}", ctx.channel().id().asShortText());
            } else if (event.state() == IdleState.ALL_IDLE) {
                log.debug("读写超时: channelId={}", ctx.channel().id().asShortText());
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}
