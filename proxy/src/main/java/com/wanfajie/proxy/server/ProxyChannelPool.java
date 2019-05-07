package com.wanfajie.proxy.server;

import com.wanfajie.proxy.HttpProxy;
import com.wanfajie.proxy.HttpProxySupplier;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.pool.ChannelHealthChecker;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.pool.FixedChannelPool;

import java.net.InetSocketAddress;

public class ProxyChannelPool extends FixedChannelPool {

    private HttpProxySupplier supplier;

    public ProxyChannelPool(Bootstrap bootstrap, HttpProxySupplier supplier, ChannelPoolHandler handler, int maxConnections) {
        super(bootstrap, handler, maxConnections);
        this.supplier = supplier;
    }

    public ProxyChannelPool(Bootstrap bootstrap, HttpProxySupplier supplier, ChannelPoolHandler handler, int maxConnections, int maxPendingAcquires) {
        super(bootstrap, handler, maxConnections, maxPendingAcquires);
        this.supplier = supplier;
    }

    public ProxyChannelPool(Bootstrap bootstrap, HttpProxySupplier supplier, ChannelPoolHandler handler, ChannelHealthChecker healthCheck, AcquireTimeoutAction action, long acquireTimeoutMillis, int maxConnections, int maxPendingAcquires) {
        super(bootstrap, handler, healthCheck, action, acquireTimeoutMillis, maxConnections, maxPendingAcquires);
        this.supplier = supplier;
    }

    public ProxyChannelPool(Bootstrap bootstrap, HttpProxySupplier supplier, ChannelPoolHandler handler, ChannelHealthChecker healthCheck, AcquireTimeoutAction action, long acquireTimeoutMillis, int maxConnections, int maxPendingAcquires, boolean releaseHealthCheck) {
        super(bootstrap, handler, healthCheck, action, acquireTimeoutMillis, maxConnections, maxPendingAcquires, releaseHealthCheck);
        this.supplier = supplier;
    }

    public ProxyChannelPool(Bootstrap bootstrap, HttpProxySupplier supplier, ChannelPoolHandler handler, ChannelHealthChecker healthCheck, AcquireTimeoutAction action, long acquireTimeoutMillis, int maxConnections, int maxPendingAcquires, boolean releaseHealthCheck, boolean lastRecentUsed) {
        super(bootstrap, handler, healthCheck, action, acquireTimeoutMillis, maxConnections, maxPendingAcquires, releaseHealthCheck, lastRecentUsed);
        this.supplier = supplier;
    }

    @Override
    protected ChannelFuture connectChannel(Bootstrap bs) {
        HttpProxy proxy = supplier.get();
        InetSocketAddress proxyAddress = proxy.getAddress();
        return bs.connect(proxyAddress);
    }
}
