package com.wanfajie.proxy.server;

import com.wanfajie.proxy.HttpProxy;
import com.wanfajie.proxy.HttpProxySupplier;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.pool.ChannelHealthChecker;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.pool.SimpleChannelPool;

import java.net.InetSocketAddress;

public class SimpleProxyChannelPool extends SimpleChannelPool {

    private final HttpProxySupplier supplier;

    public SimpleProxyChannelPool(Bootstrap bootstrap, HttpProxySupplier supplier, ChannelPoolHandler handler) {
        super(bootstrap, handler);
        this.supplier = supplier;
    }

    public SimpleProxyChannelPool(Bootstrap bootstrap, HttpProxySupplier supplier, ChannelPoolHandler handler, ChannelHealthChecker healthCheck) {
        super(bootstrap, handler, healthCheck);
        this.supplier = supplier;
    }

    public SimpleProxyChannelPool(Bootstrap bootstrap, HttpProxySupplier supplier, ChannelPoolHandler handler, ChannelHealthChecker healthCheck, boolean releaseHealthCheck) {
        super(bootstrap, handler, healthCheck, releaseHealthCheck);
        this.supplier = supplier;
    }

    public SimpleProxyChannelPool(Bootstrap bootstrap, HttpProxySupplier supplier, ChannelPoolHandler handler, ChannelHealthChecker healthCheck, boolean releaseHealthCheck, boolean lastRecentUsed) {
        super(bootstrap, handler, healthCheck, releaseHealthCheck, lastRecentUsed);
        this.supplier = supplier;
    }

    @Override
    protected ChannelFuture connectChannel(Bootstrap bs) {
        HttpProxy proxy = supplier.get();
        InetSocketAddress proxyAddress = proxy.getAddress();
        return bs.connect(proxyAddress);
    }
}
