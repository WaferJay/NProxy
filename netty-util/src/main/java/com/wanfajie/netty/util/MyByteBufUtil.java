package com.wanfajie.netty.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

public class MyByteBufUtil {

    private MyByteBufUtil() {
        throw new UnsupportedOperationException();
    }

    public static String safePrettyHexDump(ByteBuf byteBuf, int offset, int length) {
        int writerIndex = byteBuf.writerIndex();
        length = length > writerIndex ? writerIndex : length;
        return ByteBufUtil.prettyHexDump(byteBuf, offset, length);
    }
}
