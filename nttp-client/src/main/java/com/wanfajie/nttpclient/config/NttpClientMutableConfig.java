package com.wanfajie.nttpclient.config;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;

public interface NttpClientMutableConfig extends NttpClientConfig {
    NttpClientMutableConfig connectionPerServer(int seconds);
    NttpClientMutableConfig connectTimeout(int seconds);
    NttpClientMutableConfig group(EventLoopGroup group);
    NttpClientMutableConfig channelClass(Class<? extends SocketChannel> clazz);
}
