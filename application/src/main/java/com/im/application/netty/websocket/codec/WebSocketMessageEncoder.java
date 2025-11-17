package com.im.application.netty.websocket.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * WebSocket 出站消息编码器
 * 将常见的应用层消息类型转换为 WebSocket 帧：
 * - 文本消息使用 {@link TextWebSocketFrame}
 * - 二进制消息使用 {@link BinaryWebSocketFrame}
 *
 * 使用方式：在 WebSocket 管线中添加该编码器，使业务侧可以直接写入 String/byte[]/ByteBuf 等对象。
 */
public class WebSocketMessageEncoder extends MessageToMessageEncoder<Object> {

    private final Charset charset;

    /**
     * 默认使用 UTF-8 文本编码
     */
    public WebSocketMessageEncoder() {
        this(StandardCharsets.UTF_8);
    }

    /**
     * 指定文本帧的字符集
     * @param charset 字符集
     */
    public WebSocketMessageEncoder(Charset charset) {
        this.charset = charset == null ? StandardCharsets.UTF_8 : charset;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {
        if (msg == null) {
            return;
        }

        if (msg instanceof ByteBuf) {
            out.add(new BinaryWebSocketFrame((ByteBuf) msg));
            return;
        }

        if (msg instanceof byte[]) {
            out.add(new BinaryWebSocketFrame(Unpooled.wrappedBuffer((byte[]) msg)));
            return;
        }

        if (msg instanceof CharSequence) {
            out.add(new TextWebSocketFrame(Unpooled.copiedBuffer((CharSequence) msg, charset)));
            return;
        }

        // 其他对象统一按 toString 文本输出
        out.add(new TextWebSocketFrame(String.valueOf(msg)));
    }
}