package com.wanfajie.nttpclient.strategy;

import com.wanfajie.nttpclient.HttpSnoopClientInitializer;
import com.wanfajie.proxy.HttpProxy;
import com.wanfajie.proxy.client.HttpProxyInitializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

import java.net.InetSocketAddress;

public class SimpleHttpClientStrategy implements HttpClientStrategy {

    private final Bootstrap bootstrap;
    private final boolean httpsMode;

    public SimpleHttpClientStrategy(Bootstrap bootstrap, boolean httpsMode) {
        this.httpsMode = httpsMode;
        ChannelInitializer<SocketChannel> initializer = createInitializer();
        this.bootstrap = bootstrap.clone()
                .option(ChannelOption.SO_KEEPALIVE, false)
                .handler(initializer);
    }

    public SimpleHttpClientStrategy(EventLoopGroup workers, Class<? extends SocketChannel> channelClass, boolean httpsMode) {
        this(
            new Bootstrap()
                .group(workers)
                .channel(channelClass),
            httpsMode
        );
    }

    protected ChannelInitializer<SocketChannel> createInitializer() {
        ChannelInitializer<SocketChannel> initializer = new HttpSnoopClientInitializer(httpsMode);
        initializer = new HttpProxyInitializer(initializer);
        return initializer;
    }

    @Override
    public Future<Channel> createChannel(InetSocketAddress address, HttpProxy proxy) {

        Bootstrap bs = bootstrap;

        if (proxy != null) {
            bs = bs.clone();
            bs.attr(HttpProxyInitializer.PROXY_KEY, proxy);
        }

        ChannelFuture future = bs.connect(address);
        Promise<Channel> promise = future.channel().eventLoop().newPromise();

        future.addListener((ChannelFutureListener) f -> {
            if (f.isSuccess()) {
                promise.setSuccess(f.channel());
            } else {
                promise.setFailure(f.cause());
            }
        });

        return promise;
    }

    @Override
    public Future<Void> recycler(Channel channel) {
        return channel.close();
    }

    @Override
    public boolean keepAlive() {
        return false;
    }

    @Override
    public void close() {
    }
}
