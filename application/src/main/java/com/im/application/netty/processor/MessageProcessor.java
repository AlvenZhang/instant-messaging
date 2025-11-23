package com.im.application.netty.processor;
import io.netty.channel.ChannelHandlerContext;

/**
 * 消息处理器接口
 * 任何对消息的处理都会实现该接口
 * 
 * @param <T> 消息数据类型
 */
public interface MessageProcessor<T> {
    
    /**
     * 处理消息（带通道上下文）
     * 提供默认实现，调用不带通道上下文的process方法
     * 
     * @param ctx 通道处理器上下文
     * @param data 消息数据
     */
    default void process(ChannelHandlerContext ctx, T data) {
        process(data);
    }
    
    /**
     * 处理消息（不带通道上下文）
     * 提供默认空实现，子类可以选择性实现
     * 
     * @param data 消息数据
     */
    default void process(T data) {
        // 默认空实现
    }
    
    /**
     * 将对象转换为指定类型
     * 提供默认实现，简单地进行类型转换
     * 
     * @param obj 原始对象
     * @return 转换后的对象
     */
    @SuppressWarnings("unchecked")
    default T transform(Object obj) {
        return (T) obj;
    }
}
