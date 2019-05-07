package com.wanfajie.proxy.server;

import io.netty.channel.Channel;
import io.netty.channel.pool.ChannelPool;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

public class DefaultProxyLinker implements NProxyLinker {

    private final ChannelPool pool;

    public DefaultProxyLinker(ChannelPool pool) {
        this.pool = pool;
    }

    @Override
    public Future<Channel> acquire(Channel localChannel) {
        Future<Channel> future = pool.acquire();
        return linkChannel(localChannel, future);
    }

    @Override
    public Future<Channel> acquire(Channel localChannel, Promise<Channel> promise) {
        Future<Channel> future = pool.acquire(promise);
        return linkChannel(localChannel, future);
    }

    private Future<Channel> linkChannel(Channel local, Future<Channel> future) {
        return future.addListener(f -> {
            Channel pooled = (Channel) f.get();
            NProxyRemoteHandler handler = pooled.pipeline().get(NProxyRemoteHandler.class);
            handler.localChannel(local);
        });
    }

    @Override
    public Future<Void> release(Channel remoteChannel) {
        return pool.release(remoteChannel);
    }

    @Override
    public Future<Void> release(Channel remoteChannel, Promise<Void> promise) {
        return pool.release(remoteChannel, promise);
    }

    @Override
    public void close() {
        pool.close();
    }
}
