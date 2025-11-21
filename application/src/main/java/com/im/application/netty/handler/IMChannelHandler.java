package com.im.application.netty.handler;
import com.im.application.netty.cache.UserChannelContextCache;
import com.im.application.netty.processor.MessageProcessor;
import com.im.application.netty.processor.factory.ProcessorFactory;
import com.im.common.cache.distribute.DistributedCache;
import com.im.common.domain.constant.IMConstants;
import com.im.common.domain.enums.SendMessageType;
import com.im.common.domain.model.CommonSendData;
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
public class IMChannelHandler extends SimpleChannelInboundHandler<CommonSendData> {
    
    /**
     * 通道属性Key：用户ID
     */
//    public static final AttributeKey<Long> USER_ID = AttributeKey.valueOf("USER_ID");
    
    /**
     * 通道属性Key：终端类型
     */
//    public static final AttributeKey<Integer> TERMINAL_TYPE = AttributeKey.valueOf("TERMINAL_TYPE");
    
    @Autowired
    private UserChannelContextCache userChannelContextCache;
    
    @Autowired
    private ProcessorFactory processorFactory;
    
    /**
     * 处理入站事件和数据
     * 当接收到客户端发送的消息时，会调用此方法
     * 
     * @param ctx 通道处理器上下文
     * @param msg 接收到的消息
     * @throws Exception 处理过程中可能抛出的异常
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, CommonSendData msg) throws Exception {
        log.info("接收到消息: channelId={}, message={}", ctx.channel().id().asShortText(), msg);
        
        try {
            // 从通道属性中获取用户ID和终端类型
            AttributeKey<Long> userIdAttr = AttributeKey.valueOf(IMConstants.USER_ID);
            Long userId = ctx.channel().attr(userIdAttr).get();
            AttributeKey<Integer> terminalAttr = AttributeKey.valueOf(IMConstants.TERMINAL_TYPE);
            Integer terminalType = ctx.channel().attr(terminalAttr).get();
            
            // 获取对应的消息处理器
            MessageProcessor processor = processorFactory.getProcessor(msg.getCommandType());
            
            if (processor == null) {
                log.warn("未找到消息处理器: messageType={}, channelId={}", msg.getCommandType(), ctx.channel().id().asShortText());
                return;
            }
            
            // 处理消息
            if (userId != null && terminalType != null) {
                log.info("处理已认证用户消息: userId={}, terminalType={}, messageType={}", userId, terminalType, msg.getCommandType());
                processor.process(ctx, msg.getData());
            } else {
                // 未认证用户只能处理登录消息
                if (msg.getCommandType() == SendMessageType.LOGIN) {
                    log.info("处理登录消息: channelId={}", ctx.channel().id().asShortText());
                    processor.process(ctx, msg.getData());
                } else {
                    log.warn("未认证用户尝试发送非登录消息: messageType={}, channelId={}", msg.getCommandType(), ctx.channel().id().asShortText());
                    ctx.close();
                }
            }
        } catch (Exception e) {
            log.error("处理消息异常: channelId={}, message={}", ctx.channel().id().asShortText(), msg, e);
            ctx.close();
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
        AttributeKey<Long> userIdAttr = AttributeKey.valueOf(IMConstants.USER_ID);
        Long userId = ctx.channel().attr(userIdAttr).get();
        AttributeKey<Integer> terminalAttr = AttributeKey.valueOf(IMConstants.TERMINAL_TYPE);
        Integer terminalType = ctx.channel().attr(terminalAttr).get();
        
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
        AttributeKey<Long> userIdAttr = AttributeKey.valueOf(IMConstants.USER_ID);
        Long userId = ctx.channel().attr(userIdAttr).get();
        AttributeKey<Integer> terminalAttr = AttributeKey.valueOf(IMConstants.TERMINAL_TYPE);
        Integer terminalType= ctx.channel().attr(terminalAttr).get();
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
            String redisKey = String.join(IMConstants.REDIS_KEY_SPLIT, IMConstants.IM_USER_SERVER_ID, userId.toString(), terminalType.toString());
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
                AttributeKey<Long> userIdAttr = AttributeKey.valueOf(IMConstants.USER_ID);
                Long userId = ctx.channel().attr(userIdAttr).get();
                AttributeKey<Integer> terminalAttr = AttributeKey.valueOf(IMConstants.TERMINAL_TYPE);
                Integer terminalType = ctx.channel().attr(terminalAttr).get();
                
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
