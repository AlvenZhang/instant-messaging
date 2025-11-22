package com.im.application.netty;
/**
 * Netty服务器接口
 * 定义了即时通讯服务器的基本操作
 */
public interface IMNettyServer {
    
    /**
     * 判断服务是否准备就绪
     * @return true表示服务已准备就绪，false表示服务未准备就绪
     */
    boolean isReady();
    
    /**
     * 启动服务
     * @throws Exception 启动过程中可能抛出的异常
     */
    void start() throws Exception;
    
    /**
     * 停止服务
     * @throws Exception 停止过程中可能抛出的异常
     */
    void shutdown() throws Exception;
}
