package com.im.application.netty.websocket.codec;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.websocketx.*;

import java.util.List;

/**
 * WebSocket 入站消息解码器
 * 将 {@link WebSocketFrame} 解码为业务可直接使用的对象：
 * - {@link TextWebSocketFrame} → {@code String}
 * - {@link BinaryWebSocketFrame} → {@code byte[]}
 * 其他类型帧（Ping/Pong/Close）原样透传，供协议处理链路使用。
 */
public class WebSocketMessageDecoder extends MessageToMessageDecoder<WebSocketFrame> {

    @Override
    protected void decode(io.netty.channel.ChannelHandlerContext ctx, WebSocketFrame frame, List<Object> out) throws Exception {
        if (frame instanceof TextWebSocketFrame) {
            out.add(((TextWebSocketFrame) frame).text());
            return;
        }

        if (frame instanceof BinaryWebSocketFrame) {
            ByteBuf content = frame.content();
            byte[] bytes = new byte[content.readableBytes()];
            content.readBytes(bytes);
            out.add(bytes);
            return;
        }

        // 其他控制帧交由后续处理器处理（保持引用计数）
        out.add(frame.retain());
    }
}