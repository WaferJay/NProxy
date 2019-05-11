package com.wanfajie.proxy.server;

import io.netty.channel.Channel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

import java.io.Closeable;

public interface NProxyLinker extends Closeable {

    default Future<Channel> acquire(Channel localChannel) {
        return acquire(localChannel, localChannel.eventLoop().newPromise());
    }

    Future<Channel> acquire(Channel localChannel, Promise<Channel> promise);

    default Future<Void> release(Channel remoteChannel) {
        return release(remoteChannel, remoteChannel.eventLoop().newPromise());
    }

    Future<Void> release(Channel remoteChannel, Promise<Void> promise);

    void close();
}
