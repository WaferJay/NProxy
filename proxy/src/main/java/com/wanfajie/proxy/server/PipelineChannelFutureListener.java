package com.wanfajie.proxy.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.internal.logging.InternalLogger;

class PipelineChannelFutureListener implements ChannelFutureListener {

    private final Channel channel;
    private final InternalLogger logger;

    PipelineChannelFutureListener(Channel channel, InternalLogger logger) {
        this.channel = channel;
        this.logger = logger;
    }

    @Override
    public void operationComplete(ChannelFuture future) {
        if (future.isSuccess()) {
            channel.read();
        } else {
            logger.error("Write fail: {} => {}", channel, future.channel());
            logger.error(future.cause());
            future.channel().close();
        }
    }
}
