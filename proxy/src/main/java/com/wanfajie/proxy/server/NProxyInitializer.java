package com.wanfajie.proxy.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.pool.ChannelPool;
import io.netty.util.AttributeKey;

public class NProxyInitializer extends ChannelInitializer<Channel> {

    public static final AttributeKey<Channel> PROXIED_CHANNEL = AttributeKey.newInstance("proxiedChannel");

    private NProxyLinker proxyLinker;

    public NProxyInitializer(ChannelPool channelPool) {
        this.proxyLinker = new DefaultProxyLinker(channelPool);
    }

    @Override
    protected void initChannel(Channel channel) {
        ChannelHandler handler = new NProxyLocalHandler(proxyLinker);

        channel.pipeline()
               .addLast(handler);
    }
}
