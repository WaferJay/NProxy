package com.wanfajie.nttpclient.config;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;

public interface NttpClientConfig {
    int connectionPerServer();
    int connectTimeout();
    EventLoopGroup group();
    Class<? extends SocketChannel> channelClass();
}
