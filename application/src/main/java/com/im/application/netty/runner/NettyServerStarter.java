package com.im.application.netty.runner;

import com.im.application.netty.IMNettyServer;
import com.im.application.netty.tcp.TCPNettyServer;
import com.im.application.netty.websocket.WebSocketNettyServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Netty服务器启动器
 * 实现CommandLineRunner接口，在SpringBoot启动完成后自动启动Netty服务
 */
@Slf4j
@Component
public class NettyServerStarter implements CommandLineRunner {
    
    @Autowired
    private TCPNettyServer tcpNettyServer;
    
    @Autowired
    private WebSocketNettyServer webSocketNettyServer;
    
    @Value("${im.tcp.enabled:true}")
    private boolean tcpEnabled;
    
    @Value("${im.websocket.enabled:true}")
    private boolean websocketEnabled;
    
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);
    
    /**
     * 需要启动的服务器列表
     */
    private final List<IMNettyServer> servers = new ArrayList<>();
    
    @Override
    public void run(String... args) throws Exception {
        log.info("开始启动Netty服务器...");
        
        // 根据配置添加需要启动的服务器
        if (tcpEnabled) {
            servers.add(tcpNettyServer);
            log.info("TCP Netty服务器已加入启动列表");
        } else {
            log.info("TCP Netty服务器未启用");
        }
        
        if (websocketEnabled) {
            servers.add(webSocketNettyServer);
            log.info("WebSocket Netty服务器已加入启动列表");
        } else {
            log.info("WebSocket Netty服务器未启用");
        }
        
        // 统一启动所有服务器
        startServers();
        
        // 等待服务器启动完成
        waitForServersReady();
        
        log.info("Netty服务器启动完成，共启动{}个服务", servers.size());
        
        // 添加JVM关闭钩子，确保服务器优雅关闭
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("JVM正在关闭，开始关闭Netty服务器...");
            shutdownServers();
            executorService.shutdown();
            log.info("Netty服务器已关闭");
        }));
    }
    
    /**
     * 启动所有服务器
     */
    private void startServers() {
        for (IMNettyServer server : servers) {
            executorService.submit(() -> {
                try {
                    String serverName = server.getClass().getSimpleName();
                    log.info("正在启动{}...", serverName);
                    server.start();
                    log.info("{}启动成功", serverName);
                } catch (Exception e) {
                    log.error("Netty服务器启动失败: {}", server.getClass().getSimpleName(), e);
                }
            });
        }
    }
    
    /**
     * 等待服务器启动完成
     */
    private void waitForServersReady() {
        int maxWaitTime = 30; // 最大等待时间30秒
        int waitTime = 0;
        
        while (waitTime < maxWaitTime) {
            boolean allReady = servers.stream().allMatch(IMNettyServer::isReady);
            
            if (allReady) {
                log.info("所有Netty服务器已准备就绪");
                return;
            }
            
            try {
                Thread.sleep(1000);
                waitTime++;
            } catch (InterruptedException e) {
                log.error("等待服务器启动时被中断", e);
                Thread.currentThread().interrupt();
                return;
            }
        }
        
        // 输出各服务器的就绪状态
        servers.forEach(server -> {
            String serverName = server.getClass().getSimpleName();
            boolean ready = server.isReady();
            log.warn("服务器状态 - {}: {}", serverName, ready ? "就绪" : "未就绪");
        });
    }
    
    /**
     * 关闭所有Netty服务器
     */
    private void shutdownServers() {
        for (IMNettyServer server : servers) {
            try {
                String serverName = server.getClass().getSimpleName();
                log.info("正在关闭{}...", serverName);
                server.shutdown();
                log.info("{}已关闭", serverName);
            } catch (Exception e) {
                log.error("关闭Netty服务器失败: {}", server.getClass().getSimpleName(), e);
            }
        }
    }
}
