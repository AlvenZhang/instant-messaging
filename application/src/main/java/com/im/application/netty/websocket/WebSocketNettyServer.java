package com.im.application.netty.websocket;
import com.im.application.netty.IMNettyServer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * WebSocket方式的Netty服务器实现
 */
@Slf4j
@Component("webSocketNettyServer")
public class WebSocketNettyServer implements IMNettyServer {
    
    @Value("${im.websocket.port:9999}")
    private int port;
    
    @Value("${im.websocket.boss-thread:1}")
    private int bossThread;
    
    @Value("${im.websocket.worker-thread:8}")
    private int workerThread;
    
    @Value("${im.websocket.idle-time:120}")
    private int idleTime;
    
    @Value("${im.websocket.path:/ws}")
    private String websocketPath;
    
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private volatile boolean ready = false;
    
    @Override
    public boolean isReady() {
        return ready;
    }
    
    @Override
    public void start() throws Exception {
        log.info("开始启动WebSocket Netty服务器，端口: {}, 路径: {}", port, websocketPath);
        
        // 创建boss线程组，用于接收客户端连接
        bossGroup = new NioEventLoopGroup(bossThread);
        // 创建worker线程组，用于处理网络IO
        workerGroup = new NioEventLoopGroup(workerThread);
        
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    // 设置TCP连接的缓冲区大小
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    // 设置保持长连接
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    // 设置不延迟发送
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            
                            // 添加心跳检测处理器，超过idleTime秒没有读操作则触发IdleStateEvent
                            pipeline.addLast(new IdleStateHandler(idleTime, 0, 0, TimeUnit.SECONDS));
                            
                            // HTTP请求的解码和编码
                            pipeline.addLast(new HttpServerCodec());
                            
                            // 将HTTP消息的多个部分组合成一条完整的HTTP消息
                            pipeline.addLast(new HttpObjectAggregator(65536));
                            
                            // 支持大文件传输
                            pipeline.addLast(new ChunkedWriteHandler());
                            
                            // WebSocket协议处理器，处理握手、心跳、关闭等
                            // 参数为WebSocket的路径
                            pipeline.addLast(new WebSocketServerProtocolHandler(websocketPath));

                            // 添加入站解码器：将 WebSocketFrame 转为 String/byte[]
                            pipeline.addLast(new com.im.application.netty.websocket.codec.WebSocketMessageDecoder());

                            // 添加自定义出站编码器：将 String/byte[]/ByteBuf 转为 WebSocket 帧
                            pipeline.addLast(new com.im.application.netty.websocket.codec.WebSocketMessageEncoder());

                            // TODO: 添加自定义的业务处理器
                            // pipeline.addLast(new WebSocketMessageHandler());
                        }
                    });
            
            // 绑定端口并启动服务器
            ChannelFuture future = bootstrap.bind(port).sync();
            serverChannel = future.channel();
            ready = true;
            
            log.info("WebSocket Netty服务器启动成功，监听端口: {}, 路径: {}", port, websocketPath);
            
            // 监听服务器关闭
            future.channel().closeFuture().addListener((ChannelFutureListener) closeFuture -> {
                log.info("WebSocket Netty服务器已关闭");
            });
            
        } catch (Exception e) {
            log.error("WebSocket Netty服务器启动失败", e);
            shutdown();
            throw e;
        }
    }
    
    @Override
    public void shutdown() throws Exception {
        log.info("开始关闭WebSocket Netty服务器");
        ready = false;
        
        try {
            if (serverChannel != null) {
                serverChannel.close().sync();
            }
        } catch (Exception e) {
            log.error("关闭服务器Channel失败", e);
        }
        
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        
        log.info("WebSocket Netty服务器已关闭");
    }
}
