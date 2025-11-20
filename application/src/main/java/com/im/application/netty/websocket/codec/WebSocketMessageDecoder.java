package com.im.application.netty.websocket.codec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.common.domain.model.CommonSendData;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.websocketx.*;

import java.util.List;

import static java.awt.SystemColor.text;

/**
 * WebSocket 入站消息解码器
 * 将 {@link WebSocketFrame} 解码为业务可直接使用的对象：
 * - {@link TextWebSocketFrame} → {@code String}
 * - {@link BinaryWebSocketFrame} → {@code byte[]}
 * 其他类型帧（Ping/Pong/Close）原样透传，供协议处理链路使用。
 */
public class WebSocketMessageDecoder extends MessageToMessageDecoder<TextWebSocketFrame> {

    @Override
    protected void decode(io.netty.channel.ChannelHandlerContext ctx, TextWebSocketFrame textWebSocketFrame, List<Object> out) throws Exception {
        CommonSendData commonSendData = new ObjectMapper().readValue(textWebSocketFrame.text(), CommonSendData.class);
        out.add(commonSendData);
    }
}