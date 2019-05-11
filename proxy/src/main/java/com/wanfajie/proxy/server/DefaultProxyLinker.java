package com.wanfajie.proxy.server;

import com.wanfajie.netty.util.ChannelUtils;
import com.wanfajie.proxy.HttpProxy;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

import java.util.Deque;

public class DefaultProxyLinker implements NProxyLinker {

    private static final AttributeKey<HttpProxy> PROXY_KEY = AttributeKey.newInstance("proxyServerKey");

    private final Deque<HttpProxy> deque;
    private final Bootstrap bootstrap;

    public DefaultProxyLinker(Bootstrap bootstrap, Deque<HttpProxy> deque) {
        this.bootstrap = bootstrap.clone()
                .option(ChannelOption.AUTO_READ, false);

        this.deque = deque;
    }

    @Override
    public Future<Channel> acquire(Channel localChannel, Promise<Channel> promise) {
        HttpProxy proxy = pollProxy();
        NProxyRemoteHandler handler = new NProxyRemoteHandler(localChannel);

        ChannelFuture future = bootstrap.clone()
                .attr(PROXY_KEY, proxy)
                .handler(handler)
                .connect(proxy.getAddress());

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
    public Future<Void> release(Channel remoteChannel, Promise<Void> promise) {
        HttpProxy proxy = remoteChannel.attr(PROXY_KEY).get();

        if (remoteChannel.isActive()) {
            ChannelUtils.closeOnFlush(remoteChannel);
        }

        if (proxy == null) {
            promise.setFailure(new IllegalArgumentException("no proxy attribute"));
            return promise;
        }

        if (offerProxy(proxy)) {
            promise.setSuccess(null);
        } else {
            promise.setFailure(new IllegalStateException("offer fail"));
        }
        return promise;
    }

    private HttpProxy pollProxy() {
        return deque.poll();
    }

    private boolean offerProxy(HttpProxy proxy) {
        return deque.offer(proxy);
    }

    @Override
    public void close() {
        deque.clear();
    }
}
