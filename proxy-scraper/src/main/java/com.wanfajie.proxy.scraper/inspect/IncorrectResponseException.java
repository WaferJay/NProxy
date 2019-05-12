package com.wanfajie.proxy.scraper.inspect;

import com.wanfajie.netty.util.MyByteBufUtil;
import io.netty.buffer.ByteBuf;

public class IncorrectResponseException extends Exception {

    public IncorrectResponseException(ByteBuf byteBuf) {
        super("\n" + MyByteBufUtil.safePrettyHexDump(byteBuf, 0, 128));
    }
}
