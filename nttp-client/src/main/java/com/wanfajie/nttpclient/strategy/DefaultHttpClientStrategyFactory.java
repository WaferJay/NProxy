package com.wanfajie.nttpclient.strategy;

import com.wanfajie.nttpclient.config.NttpClientConfig;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;

public class DefaultHttpClientStrategyFactory implements HttpClientStrategyFactory {

    @Override
    public HttpClientStrategy create(NttpClientConfig config, boolean httpsMode) {
        int connectionPerServer = config.connectionPerServer();
        int connectTimeoutSeconds = config.connectTimeout();
        EventLoopGroup group = config.group();

        Bootstrap bs = new Bootstrap()
                .group(group)
                .channel(config.channelClass());

        if (connectTimeoutSeconds > 0) {
            bs.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutSeconds * 1000);
        }

        if (connectionPerServer > 0) {
            return new PooledHttpClientStrategy(bs, group, connectionPerServer, httpsMode);
        } else {
            return new SimpleHttpClientStrategy(bs, httpsMode);
        }
    }
}
