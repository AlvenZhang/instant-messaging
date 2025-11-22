package com.im.application.netty.websocket.codec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.common.domain.model.CommonSendData;
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
public class WebSocketMessageEncoder extends MessageToMessageEncoder<CommonSendData> {

    @Override
    protected void encode(ChannelHandlerContext ctx, CommonSendData commonSendData, List<Object> out) throws Exception {
        out.add(new TextWebSocketFrame(new ObjectMapper().writeValueAsString(commonSendData)));
    }
}