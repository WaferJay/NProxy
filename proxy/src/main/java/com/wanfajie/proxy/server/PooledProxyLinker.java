package com.wanfajie.proxy.server;

import io.netty.channel.Channel;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import io.netty.util.internal.ObjectUtil;

public class PooledProxyLinker implements NProxyLinker {

    private final ChannelPool pool;

    public PooledProxyLinker(ChannelPool pool) {
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

            if (f.isSuccess()) {
                Channel pooled = (Channel) f.get();
                linkChannel(local, pooled);
            }
        });
    }

    private static void linkChannel(Channel local, Channel remote) {
        NProxyRemoteHandler handler = remote.pipeline().get(NProxyRemoteHandler.class);
        handler.localChannel(local);
    }

    @Override
    public Future<Void> release(Channel remoteChannel, Promise<Void> promise) {
        return pool.release(remoteChannel, promise);
    }

    @Override
    public void close() {
        pool.close();
    }

    public static final ChannelPoolHandler CHANNEL_POOL_HANDLER = new ChannelPoolHandler() {

        @Override
        public void channelReleased(Channel channel) {

            if (channel.isActive()) {
                linkChannel(null, channel);
            }
        }

        @Override
        public void channelAcquired(Channel channel) {

        }

        @Override
        public void channelCreated(Channel channel) {
            channel.pipeline()
                   .addLast(new NProxyRemoteHandler());
        }
    };

    public static ChannelPoolHandler channelPoolHandler(final ChannelPoolHandler channelPoolHandler) {
        ObjectUtil.checkNotNull(channelPoolHandler, "channelPoolHandler");

        return new ChannelPoolHandler() {
            @Override
            public void channelReleased(Channel channel) throws Exception {
                if (channel.isActive()) {
                    linkChannel(null, channel);
                }
                channelPoolHandler.channelReleased(channel);
            }

            @Override
            public void channelAcquired(Channel channel) throws Exception {
                channelPoolHandler.channelAcquired(channel);
            }

            @Override
            public void channelCreated(Channel channel) throws Exception {
                channel.pipeline()
                       .addLast(new NProxyRemoteHandler());

                channelPoolHandler.channelCreated(channel);
            }
        };
    }
}
