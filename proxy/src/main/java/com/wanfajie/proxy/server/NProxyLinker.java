package com.wanfajie.proxy.server;

import io.netty.channel.Channel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

import java.io.Closeable;

public interface NProxyLinker extends Closeable {

    Future<Channel> acquire(Channel localChannel);

    Future<Channel> acquire(Channel localChannel, Promise<Channel> promise);

    Future<Void> release(Channel remoteChannel);

    Future<Void> release(Channel remoteChannel, Promise<Void> promise);

    void close();
}
