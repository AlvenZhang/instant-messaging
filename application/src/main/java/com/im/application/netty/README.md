# Netty即时通讯服务实现
## 概述

本包实现了基于Netty的即时通讯服务，支持TCP和WebSocket两种连接方式。

## 核心组件

### 1. IMNettyServer接口

定义了Netty服务器的基本操作：

- `isReady()`: 判断服务是否准备就绪
- `start()`: 启动服务
- `shutdown()`: 停止服务

### 2. TCPNettyServer

TCP方式的Netty服务器实现，特性：

- 基于TCP协议的长连接
- 使用长度字段解决粘包/拆包问题
- 支持心跳检测机制
- 可配置线程池大小和超时时间

### 3. WebSocketNettyServer

WebSocket方式的Netty服务器实现，特性：

- 基于WebSocket协议
- 支持HTTP升级握手
- 支持大文件传输
- 支持心跳检测机制
- 可配置WebSocket路径

### 4. UserChannelContextCache

用户连接上下文缓存，用于管理用户连接：

- 存储用户ID、终端类型与连接的映射关系
- 支持多终端同时在线
- 提供连接的增删改查操作
- 线程安全的实现

主要方法：

- `put(userId, terminalType, ctx)`: 存储用户连接
- `remove(userId, terminalType)`: 移除指定终端连接
- `removeAll(userId)`: 移除用户所有连接
- `get(userId, terminalType)`: 获取指定终端连接
- `getAll(userId)`: 获取用户所有终端连接
- `isOnline(userId, terminalType)`: 判断用户是否在线
- `getOnlineUserCount()`: 获取在线用户数
- `getTotalConnectionCount()`: 获取总连接数

### 5. NettyServerStarter

Netty服务器启动器，实现了`CommandLineRunner`接口：

- 在SpringBoot启动完成后自动启动Netty服务
- 支持同时启动TCP和WebSocket服务
- 使用线程池异步启动服务
- 添加JVM关闭钩子确保优雅关闭

## 配置说明

在`application.yml`中配置：

```yaml
im:
  # TCP服务配置
  tcp:
    enabled: true              # 是否启用TCP服务
    port: 8888                 # TCP服务端口
    boss-thread: 1             # Boss线程数
    worker-thread: 8           # Worker线程数
    idle-time: 120             # 空闲超时时间(秒)
  
  # WebSocket服务配置
  websocket:
    enabled: true              # 是否启用WebSocket服务
    port: 9999                 # WebSocket服务端口
    path: /ws                  # WebSocket路径
    boss-thread: 1             # Boss线程数
    worker-thread: 8           # Worker线程数
    idle-time: 120             # 空闲超时时间(秒)
```

## 使用示例

### 1. 启动服务

服务会在SpringBoot启动时自动启动，无需手动调用。

### 2. 管理用户连接

```java
@Autowired
private UserChannelContextCache channelCache;

// 存储用户连接
channelCache.put(userId, terminalType, ctx);

// 获取用户连接
ChannelHandlerContext ctx = channelCache.get(userId, terminalType);

// 判断用户是否在线
boolean online = channelCache.isOnline(userId, terminalType);

// 移除用户连接
channelCache.remove(userId, terminalType);
```

### 3. 发送消息给用户

```java
ChannelHandlerContext ctx = channelCache.get(userId, terminalType);
if (ctx != null && ctx.channel().isActive()) {
    ctx.writeAndFlush(message);
}
```

## 后续扩展

当前实现中，业务处理器部分使用TODO标记，需要根据实际业务需求实现：

1. **TCP消息处理器**: 在`TCPNettyServer`中添加自定义的`TCPMessageHandler`
2. **WebSocket消息处理器**: 在`WebSocketNettyServer`中添加自定义的`WebSocketMessageHandler`

这些处理器需要实现：

- 用户认证
- 消息编解码
- 业务逻辑处理
- 异常处理
- 连接管理（与UserChannelContextCache集成）

## 注意事项

1. 确保配置的端口未被占用
2. 根据实际负载调整线程池大小
3. 合理设置心跳超时时间
4. 生产环境建议添加SSL/TLS支持
5. 需要实现具体的消息处理器才能处理业务逻辑
