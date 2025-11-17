package com.im.application.netty.tcp;
import com.im.application.netty.IMNettyServer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * TCP方式的Netty服务器实现
 */
@Slf4j
@Component("tcpNettyServer")
public class TCPNettyServer implements IMNettyServer {
    
    @Value("${im.tcp.port:8888}")
    private int port;
    
    @Value("${im.tcp.boss-thread:1}")
    private int bossThread;
    
    @Value("${im.tcp.worker-thread:8}")
    private int workerThread;
    
    @Value("${im.tcp.idle-time:120}")
    private int idleTime;
    
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
        log.info("开始启动TCP Netty服务器，端口: {}", port);
        
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
                            
                            // 添加基于长度字段的帧解码器，解决TCP粘包/拆包问题
                            // maxFrameLength: 最大帧长度
                            // lengthFieldOffset: 长度字段偏移量
                            // lengthFieldLength: 长度字段占用字节数
                            // lengthAdjustment: 长度调整值
                            // initialBytesToStrip: 跳过的字节数
                            pipeline.addLast(new LengthFieldBasedFrameDecoder(
                                    65536, 0, 4, 0, 4));
                            
                            // 添加长度字段编码器，在消息前添加4字节的长度字段
                            pipeline.addLast(new LengthFieldPrepender(4));
                            
                            // TODO: 添加自定义的业务处理器
                            // pipeline.addLast(new TCPMessageHandler());
                        }
                    });
            
            // 绑定端口并启动服务器
            ChannelFuture future = bootstrap.bind(port).sync();
            serverChannel = future.channel();
            ready = true;
            
            log.info("TCP Netty服务器启动成功，监听端口: {}", port);
            
            // 监听服务器关闭
            future.channel().closeFuture().addListener((ChannelFutureListener) closeFuture -> {
                log.info("TCP Netty服务器已关闭");
            });
            
        } catch (Exception e) {
            log.error("TCP Netty服务器启动失败", e);
            shutdown();
            throw e;
        }
    }
    
    @Override
    public void shutdown() throws Exception {
        log.info("开始关闭TCP Netty服务器");
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
        
        log.info("TCP Netty服务器已关闭");
    }
}
