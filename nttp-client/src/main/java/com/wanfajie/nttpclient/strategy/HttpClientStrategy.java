package com.wanfajie.nttpclient.strategy;

import com.wanfajie.proxy.HttpProxy;
import io.netty.channel.Channel;
import io.netty.util.concurrent.Future;

import java.net.InetSocketAddress;

/**
 * 生产、复用连接
 * @author WaferJay
 */
public interface HttpClientStrategy {

    Future<Channel> createChannel(InetSocketAddress address, HttpProxy proxy);
    Future<Void> recycler(Channel channel);

    default Future<Channel> createChannel(InetSocketAddress address) {
        return createChannel(address, null);
    }

    default Future<Channel> createChannel(String host, int port) {
        return createChannel(host, port, null);
    }

    default Future<Channel> createChannel(String host, int port, HttpProxy proxy) {
        InetSocketAddress address = InetSocketAddress.createUnresolved(host, port);
        return createChannel(address, proxy);
    }

    boolean keepAlive();

    void close();
}
