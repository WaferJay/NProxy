package com.wanfajie.proxy.server;

import com.wanfajie.netty.util.ChannelUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public class NProxyRemoteHandler extends ChannelInboundHandlerAdapter {

    private final InternalLogger logger = InternalLoggerFactory.getInstance(NProxyRemoteHandler.class);

    private Channel channel;
    private Channel inboundChannel;
    private ChannelHandlerContext context;
    private ChannelFutureListener listener;

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        context = ctx;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        channel = ctx.channel();
        listener = new PipelineChannelFutureListener(channel, logger);
        ctx.read();
        ctx.fireChannelActive();
    }

    public void localChannel(final Channel localChannel) {
        context.executor().submit(() -> {
            logger.debug("New channel: {}", localChannel);
            inboundChannel = localChannel;
        });
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        inboundChannel.writeAndFlush(msg).addListener(listener);
        if (msg instanceof ByteBuf) {
            ByteBuf byteBuf = (ByteBuf) msg;
            int size = byteBuf.readableBytes();
            logger.info("Receive {] bytes: {} => {}", size, channel, inboundChannel);
            if (logger.isDebugEnabled()) {
                String dump = ByteBufUtil.prettyHexDump(byteBuf, 0, 64);
                logger.debug("Dump {} => {}: {}", channel, inboundChannel, dump);
            }
        } else {
            logger.info("Receive {}: {} => {}", msg, channel, inboundChannel);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        ChannelUtils.closeOnFlush(inboundChannel);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ChannelUtils.closeOnFlush(ctx.channel());
    }
}
