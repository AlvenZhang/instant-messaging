package com.im.application.netty.tcp.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * TCP 入站消息解码器
 * 在 {@code LengthFieldBasedFrameDecoder} 之后，将完整帧的负载转换为业务可直接使用的对象。
 *
 * 默认行为：将负载解码为 UTF-8 文本 {@code String}。
 * 可选二进制模式：构造时传入 {@code binaryMode=true}，则输出 {@code byte[]}。
 */
public class TCPMessageDecoder extends ByteToMessageDecoder {

    private final Charset charset;
    private final boolean binaryMode;

    /**
     * 文本模式（默认 UTF-8）
     */
    public TCPMessageDecoder() {
        this(StandardCharsets.UTF_8, false);
    }

    /**
     * 指定字符集的文本模式
     * @param charset 字符集
     */
    public TCPMessageDecoder(Charset charset) {
        this(charset, false);
    }

    /**
     * 指定是否二进制模式
     * @param charset 字符集（文本模式使用），二进制模式可传 {@code null}
     * @param binaryMode 是否输出为二进制字节数组
     */
    public TCPMessageDecoder(Charset charset, boolean binaryMode) {
        this.charset = charset == null ? StandardCharsets.UTF_8 : charset;
        this.binaryMode = binaryMode;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        int readable = in.readableBytes();
        if (readable <= 0) {
            return;
        }

        byte[] data = new byte[readable];
        in.readBytes(data);

        if (binaryMode) {
            out.add(data);
        } else {
            out.add(new String(data, charset));
        }
    }
}