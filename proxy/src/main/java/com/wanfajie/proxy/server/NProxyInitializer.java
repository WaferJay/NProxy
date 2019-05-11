package com.wanfajie.proxy.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;

public class NProxyInitializer extends ChannelInitializer<Channel> {

    private NProxyLinker proxyLinker;

    public NProxyInitializer(NProxyLinker linker) {
        this.proxyLinker = linker;
    }

    @Override
    protected void initChannel(Channel channel) {
        ChannelHandler handler = new NProxyLocalHandler(proxyLinker);

        channel.pipeline()
               .addLast(handler);
    }
}
