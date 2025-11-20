package com.im.application.netty.tcp.codec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.common.domain.model.CommonSendData;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * TCP 出站消息编码器
 * 将应用层的常见消息类型（String、byte[]、ByteBuf、其他对象的 toString）
 * 编码为字节流，交由后续 {@code LengthFieldPrepender} 等处理器进行帧封装。
 *
 * 使用方式：在 TCP 管线中添加该编码器，并置于 {@code LengthFieldPrepender} 之前。
 */
public class TCPMessageEncoder extends MessageToByteEncoder<CommonSendData> {

    private final Charset charset;

    /**
     * 使用 UTF-8 作为默认编码
     */
    public TCPMessageEncoder() {
        this(StandardCharsets.UTF_8);
    }

    /**
     * 指定字符串编码
     * @param charset 字符集
     */
    public TCPMessageEncoder(Charset charset) {
        this.charset = charset == null ? StandardCharsets.UTF_8 : charset;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, CommonSendData msg, ByteBuf out) throws Exception {
        String content = new ObjectMapper().writeValueAsString(msg);
        out.writeBytes(content.getBytes(charset));
    }
}