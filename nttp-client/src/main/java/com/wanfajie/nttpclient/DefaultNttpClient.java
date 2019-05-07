package com.wanfajie.nttpclient;

import com.wanfajie.nttpclient.exception.SchemaException;
import com.wanfajie.nttpclient.strategy.HttpClientChannelStrategy;
import com.wanfajie.nttpclient.strategy.PooledHttpClientChannelStrategy;
import com.wanfajie.nttpclient.strategy.SimpleHttpClientChannelStrategy;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.internal.ObjectUtil;

import java.net.InetSocketAddress;

public class DefaultNttpClient implements NttpClient {

    private final HttpClientChannelStrategy httpStrategy;
    private final HttpClientChannelStrategy httpsStrategy;

    public DefaultNttpClient(EventLoopGroup worker, Class<? extends SocketChannel> channelClass) {
        this(worker, channelClass, 0, 0);
    }

    public DefaultNttpClient(EventLoopGroup worker, Class<? extends SocketChannel> channelClass,
                             int maxConnectionPoolSize, int connectTimeoutSeconds) {

        ObjectUtil.checkNotNull(worker, "worker");
        ObjectUtil.checkNotNull(channelClass, "channelClass");
        ObjectUtil.checkPositiveOrZero(maxConnectionPoolSize, "maxConnectionPoolSize");
        ObjectUtil.checkPositiveOrZero(connectTimeoutSeconds, "connectTimeoutSeconds");

        Bootstrap bs = new Bootstrap()
                .group(worker)
                .channel(channelClass);

        if (connectTimeoutSeconds > 0) {
            bs.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutSeconds * 1000);
        }

        if (maxConnectionPoolSize > 0) {
            this.httpStrategy = new PooledHttpClientChannelStrategy(bs, worker, maxConnectionPoolSize, false);
            this.httpsStrategy = new PooledHttpClientChannelStrategy(bs, worker, maxConnectionPoolSize, true);
        } else {
            this.httpStrategy = new SimpleHttpClientChannelStrategy(bs, false);
            this.httpsStrategy = new SimpleHttpClientChannelStrategy(bs, true);
        }
    }

    @Override
    public HttpRequestFlow send(InetSocketAddress address, String schema, FullHttpRequest request) {
        schema = schema.toUpperCase();
        HttpClientChannelStrategy strategy;
        if ("HTTP".equals(schema)) {
            strategy = httpStrategy;
        } else if ("HTTPS".equals(schema)) {
            strategy = httpsStrategy;
        } else {
            throw new SchemaException(schema);
        }

        return new DefaultHttpRequestFlow(strategy, address, request);
    }

    @Override
    public void close() {
        httpStrategy.close();
        httpsStrategy.close();
    }
}
