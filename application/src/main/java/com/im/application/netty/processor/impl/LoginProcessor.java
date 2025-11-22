package com.im.application.netty.processor.impl;
import com.alibaba.fastjson.JSON;
import com.im.application.netty.cache.UserChannelContextCache;
import com.im.application.netty.processor.MessageProcessor;
import com.im.common.cache.distribute.DistributedCache;
import com.im.common.domain.constant.IMConstants;
import com.im.common.domain.enums.TerminalType;
import com.im.common.domain.jwt.JwtUtils;
import com.im.common.domain.model.LoginInfo;
import com.im.common.domain.model.SessionInfo;
import com.im.infrastructure.holder.SpringContextHolder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 登录消息处理器
 * 实现MessageProcessor接口，主要处理登陆消息逻辑
 * 
 * 处理流程：
 * 1. 校验Token，校验不通过则关闭当前链接
 * 2. 校验通过则通过Session获得用户ID和用户终端
 * 3. 处理异地登录逻辑
 * 4. 缓存用户终端与即时通讯后端服务建立的链接
 * 5. 设置用户和终端属性
 * 6. 初始化心跳次数
 * 7. 缓存与用户终端建立链接的即时通讯后端服务ID
 * 8. 响应客户端登录的信息
 */
@Slf4j
@Component
public class LoginProcessor implements MessageProcessor<LoginInfo> {
    
    @Autowired
    private UserChannelContextCache userChannelContextCache;

    @Value("${jwt.accessToken.secret}")
    private String accessTokenSecret;

    @Value("${server.id}")
    private Long serverId;
    
    /**
     * 处理登录消息（带通道上下文）
     * 
     * @param ctx 通道处理器上下文
     * @param data 登录信息对象，包含token信息
     */
    @Override
    public void process(ChannelHandlerContext ctx, LoginInfo data) {
        log.info("处理登录消息: channelId={}, data={}", ctx.channel().id().asShortText(), data);
        
        try {
            // 1. 验证登录信息不为空
            if (data == null || data.getToken() == null) {
                log.error("登录信息为空: channelId={}", ctx.channel().id().asShortText());
                sendLoginResponse(ctx, false, "登录信息为空");
                ctx.close();
                return;
            }
            
            String token = data.getToken();
            
            // 2. 校验Token
            if (!JwtUtils.checkSign(token, accessTokenSecret)){
                ctx.channel().close();
                log.warn("LoginProcessor.process|用户登录信息校验未通过,强制用户下线,token:{}", data.getToken());
            }
            
            // 3. 从Token中获取用户ID和终端类型
            String info = JwtUtils.getInfo(token);
            SessionInfo sessionInfo = JSON.parseObject(info, SessionInfo.class);
            Long userId = sessionInfo.getUserId();
            TerminalType terminalType = sessionInfo.getTerminal();

            if (userId == null || terminalType == null) {
                log.error("无法从Token中获取用户信息: channelId={}", ctx.channel().id().asShortText());
                sendLoginResponse(ctx, false, "无法从Token中获取用户信息");
                ctx.close();
                return;
            }
            
            log.info("用户登录: userId={}, terminalType={}, channelId={}", userId, terminalType, ctx.channel().id().asShortText());
            
            // 4. 处理异地登录逻辑
            handleMultiTerminalLogin(userId, terminalType.getCode(), ctx);
            
            // 5. 缓存用户终端与即时通讯后端服务建立的链接
            userChannelContextCache.put(userId, terminalType.getCode(), ctx);
            
            // 6. 设置用户和终端属性
            setChannelAttributes(ctx, userId, terminalType.getCode());
            
            // 7. 初始化心跳次数
            initializeHeartbeat(ctx);
            
            // 8. 缓存与用户终端建立链接的即时通讯后端服务ID
            cacheServerInfo(userId, terminalType.getCode());
            
            // 9. 响应客户端登录成功
            sendLoginResponse(ctx, true, "登录成功");
            
            log.info("用户登录成功: userId={}, terminalType={}, channelId={}", userId, terminalType, ctx.channel().id().asShortText());
            
        } catch (Exception e) {
            log.error("处理登录消息异常: channelId={}", ctx.channel().id().asShortText(), e);
            sendLoginResponse(ctx, false, "处理登录消息异常");
            ctx.close();
        }
    }
    
    /**
     * 处理登录消息（不带通道上下文）
     * 
     * @param data 登录信息对象
     */
    @Override
    public void process(LoginInfo data) {
        log.warn("调用了不带通道上下文的process方法，登录消息需要通道上下文");
    }
    
    /**
     * 将对象转换为LoginInfo类型
     * 
     * @param obj 原始对象
     * @return 转换后的LoginInfo
     */
    @Override
    public LoginInfo transform(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof LoginInfo) {
            return (LoginInfo) obj;
        }
        if (obj instanceof String) {
            // 如果是字符串，假设是token，创建LoginInfo对象
            return new LoginInfo((String) obj);
        }
        return null;
    }

    /**
     * 从Token中提取用户ID
     * 
     * @param token JWT Token
     * @return 用户ID
     */
    private Long extractUserIdFromToken(String token) {
        try {
            // TODO: 实现从Token中提取用户ID的逻辑
            // 这里简单地返回一个示例用户ID
            // 实际项目中应使用JWT库解析token并提取userId
            // 例如：JwtTokenProvider.getUserIdFromToken(token)
            
            // 示例：假设token中包含userId信息
            // 实际应该通过JWT解析获取
            if (token != null && token.length() > 0) {
                // 这里简单地使用token的hash值作为userId（仅用于演示）
                return Math.abs((long) token.hashCode());
            }
            return null;
        } catch (Exception e) {
            log.error("提取用户ID异常: token={}", token, e);
            return null;
        }
    }
    
    /**
     * 从Token中提取终端类型
     * 
     * @param token JWT Token
     * @return 终端类型
     */
    private Integer extractTerminalTypeFromToken(String token) {
        try {
            // TODO: 实现从Token中提取终端类型的逻辑
            // 这里简单地返回一个示例终端类型
            // 实际项目中应使用JWT库解析token并提取terminalType
            // 例如：JwtTokenProvider.getTerminalTypeFromToken(token)
            
            // 示例：假设token中包含terminalType信息
            // 实际应该通过JWT解析获取
            if (token != null && token.length() > 0) {
                // 这里简单地返回1作为示例（1=Android, 2=iOS, 3=Web等）
                return 1;
            }
            return null;
        } catch (Exception e) {
            log.error("提取终端类型异常: token={}", token, e);
            return null;
        }
    }
    
    /**
     * 处理异地登录逻辑
     * 如果用户已经在其他地方登录，则关闭旧连接
     * 
     * @param userId 用户ID
     * @param terminalType 终端类型
     * @param newCtx 新连接的通道上下文
     */
    private void handleMultiTerminalLogin(Long userId, Integer terminalType, ChannelHandlerContext newCtx) {
        try {
            // 获取该用户该终端的旧连接
            ChannelHandlerContext oldCtx = userChannelContextCache.get(userId, terminalType);
            
            if (oldCtx != null && oldCtx.channel().isActive()) {
                log.warn("检测到异地登录: userId={}, terminalType={}, oldChannelId={}, newChannelId={}", 
                        userId, terminalType, oldCtx.channel().id().asShortText(), newCtx.channel().id().asShortText());
                
                // 向旧连接发送强制下线消息
                sendForceOfflineMessage(oldCtx);
                
                // 关闭旧连接
                oldCtx.close();
                
                log.info("已关闭旧连接: userId={}, terminalType={}, oldChannelId={}", 
                        userId, terminalType, oldCtx.channel().id().asShortText());
            }
        } catch (Exception e) {
            log.error("处理异地登录异常: userId={}, terminalType={}", userId, terminalType, e);
        }
    }
    
    /**
     * 设置通道属性
     * 
     * @param ctx 通道处理器上下文
     * @param userId 用户ID
     * @param terminalType 终端类型
     */
    private void setChannelAttributes(ChannelHandlerContext ctx, Long userId, Integer terminalType) {
        try {
            AttributeKey<Long> userIdAttr = AttributeKey.valueOf(IMConstants.USER_ID);
            ctx.channel().attr(userIdAttr).set(userId);
            
            AttributeKey<Integer> terminalAttr = AttributeKey.valueOf(IMConstants.TERMINAL_TYPE);
            ctx.channel().attr(terminalAttr).set(terminalType);
            
            log.debug("设置通道属性成功: userId={}, terminalType={}", userId, terminalType);
        } catch (Exception e) {
            log.error("设置通道属性异常: userId={}, terminalType={}", userId, terminalType, e);
        }
    }
    
    /**
     * 初始化心跳次数
     * 
     * @param ctx 通道处理器上下文
     */
    private void initializeHeartbeat(ChannelHandlerContext ctx) {
        try {
            // 初始化心跳计数器
            AttributeKey<Integer> heartbeatCountAttr = AttributeKey.valueOf("HEARTBEAT_COUNT");
            ctx.channel().attr(heartbeatCountAttr).set(0);
            
            log.debug("初始化心跳次数成功");
        } catch (Exception e) {
            log.error("初始化心跳次数异常", e);
        }
    }
    
    /**
     * 缓存服务器信息
     * 将用户与即时通讯后端服务ID的映射关系存储到分布式缓存中
     * 
     * @param userId 用户ID
     * @param terminalType 终端类型
     */
    private void cacheServerInfo(Long userId, Integer terminalType) {
        try {
            DistributedCache distributedCache = SpringContextHolder.getBean(IMConstants.DISTRIBUTED_CACHE_REDIS_SERVICE_KEY);
            
            // 构建Redis Key
            String redisKey = String.join(IMConstants.REDIS_KEY_SPLIT, 
                    IMConstants.IM_USER_SERVER_ID, userId.toString(), terminalType.toString());
            

            // 存储到分布式缓存，设置过期时间（例如24小时）
            distributedCache.set(redisKey, serverId, Duration.ofSeconds(24 * 60 * 60));
            
            log.info("缓存服务器信息成功: userId={}, terminalType={}, serverId={}", userId, terminalType, serverId);
        } catch (Exception e) {
            log.error("缓存服务器信息异常: userId={}, terminalType={}", userId, terminalType, e);
        }
    }
    
    /**
     * 获取当前服务ID
     * 
     * @return 服务ID
     */
    private String getServerId() {
        // TODO: 实现获取当前服务ID的逻辑
        // 可以从配置文件、环境变量或其他地方获取
        // 这里简单地返回一个示例值
        return "server-001";
    }
    
    /**
     * 发送登录响应
     * 
     * @param ctx 通道处理器上下文
     * @param success 登录是否成功
     * @param message 响应消息
     */
    private void sendLoginResponse(ChannelHandlerContext ctx, boolean success, String message) {
        try {
            String response = String.format("{\"type\":\"login_response\",\"success\":%b,\"message\":\"%s\"}", 
                    success, message);
            ctx.writeAndFlush(response);
            log.debug("发送登录响应: success={}, message={}", success, message);
        } catch (Exception e) {
            log.error("发送登录响应异常", e);
        }
    }
    
    /**
     * 发送强制下线消息
     * 
     * @param ctx 通道处理器上下文
     */
    private void sendForceOfflineMessage(ChannelHandlerContext ctx) {
        try {
            String message = "{\"type\":\"force_offline\",\"message\":\"您的账号在其他地方登录，已被强制下线\"}";
            ctx.writeAndFlush(message);
            log.debug("发送强制下线消息");
        } catch (Exception e) {
            log.error("发送强制下线消息异常", e);
        }
    }
    
}
