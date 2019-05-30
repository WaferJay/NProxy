package com.wanfajie.proxy.server;

import com.wanfajie.netty.util.ChannelUtils;
import com.wanfajie.proxy.HttpProxy;
import com.wanfajie.proxy.HttpProxySupplier;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

public class DefaultProxyLinker implements NProxyLinker {

    private static final AttributeKey<HttpProxy> PROXY_KEY = AttributeKey.newInstance("proxyServerKey");

    private final HttpProxySupplier supplier;
    private final Bootstrap bootstrap;

    public DefaultProxyLinker(Bootstrap bootstrap, HttpProxySupplier supplier) {
        this.bootstrap = bootstrap;
        this.supplier = supplier;
    }

    @Override
    public Future<Channel> acquire(Channel localChannel, Promise<Channel> promise) {
        supplier.get().addListener(f -> {

            if (f.isSuccess()) {
                HttpProxy proxy = (HttpProxy) f.get();

                NProxyRemoteHandler handler = new NProxyRemoteHandler(localChannel);

                ChannelFuture future = bootstrap.clone()
                        .attr(PROXY_KEY, proxy)
                        .handler(handler)
                        .connect(proxy.getAddress());

                future.addListener((ChannelFutureListener) ff -> {
                    if (ff.isSuccess()) {
                        promise.setSuccess(ff.channel());
                    } else {
                        promise.setFailure(ff.cause());
                    }
                });
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

        return promise.setSuccess(null);
    }

    @Override
    public void close() {
    }
}
