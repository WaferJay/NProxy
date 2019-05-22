package com.wanfajie.proxy.server;

import com.wanfajie.netty.util.ChannelUtils;
import com.wanfajie.netty.util.MyByteBufUtil;
import io.netty.buffer.ByteBuf;
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

    public NProxyRemoteHandler(Channel localChannel) {
        this.inboundChannel = localChannel;
    }

    NProxyRemoteHandler() {}

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) {
        context = ctx;
        ctx.fireChannelRegistered();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        channel = ctx.channel();
        logger.info("Connected remote {}", channel);
        listener = new PipelineChannelFutureListener(channel, logger);
        ctx.read();
        ctx.fireChannelActive();
    }

    void localChannel(final Channel localChannel) {
        if (context.channel().eventLoop().inEventLoop()) {
            logger.debug("Set new channel {} for {}", localChannel, channel);
            inboundChannel = localChannel;
        } else {
            context.executor().submit(() -> {
                logger.debug("Set new channel {} for {}", localChannel, channel);
                inboundChannel = localChannel;
            });
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {

        if (logger.isInfoEnabled()) {

            if (msg instanceof ByteBuf) {
                ByteBuf byteBuf = (ByteBuf) msg;
                int size = byteBuf.readableBytes();
                logger.info("Received {} bytes: {} => {}", size, channel, inboundChannel);
                if (logger.isTraceEnabled()) {
                    String dump = MyByteBufUtil.safePrettyHexDump(byteBuf, 0, 128);
                    logger.trace("Dump {} => {}:\n{}", channel, inboundChannel, dump);
                }
            } else {
                logger.info("Received message {}: {} => {}", msg, channel, inboundChannel);
            }
        }

        inboundChannel.writeAndFlush(msg).addListener(listener);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        logger.info("Disconnected remote {}", channel);
        ChannelUtils.closeOnFlush(inboundChannel);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("exception caught {} - {}", inboundChannel, channel, cause);
        ChannelUtils.closeOnFlush(ctx.channel());
    }
}
