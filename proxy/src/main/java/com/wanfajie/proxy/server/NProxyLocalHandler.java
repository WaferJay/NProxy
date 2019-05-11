package com.wanfajie.proxy.server;

import com.wanfajie.netty.util.ChannelUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.Promise;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public class NProxyLocalHandler extends ChannelInboundHandlerAdapter {

    private final InternalLogger logger = InternalLoggerFactory.getInstance(NProxyLocalHandler.class);
    private final NProxyLinker linker;

    private Channel inboundChannel;
    private Channel outboundChannel;
    private ChannelFutureListener listener;

    public NProxyLocalHandler(NProxyLinker linker) {
        this.linker = linker;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        inboundChannel = ctx.channel();
        listener = new PipelineChannelFutureListener(ctx.channel(), logger);

        Promise<Channel> promise = ctx.channel().eventLoop().newPromise();
        linker.acquire(inboundChannel, promise).addListener(f -> {

            if (f.isSuccess()) {
                outboundChannel = (Channel) f.get();

                logger.debug("Acquired: {} => {}", inboundChannel, outboundChannel);
                inboundChannel.read();
            } else {
                // TODO: 失败重试
                logger.error("Acquire fail [{}]", inboundChannel, f.cause());
                inboundChannel.close();
            }
        });
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {

        if (outboundChannel == null) {
            throw new IllegalStateException();
        }

        if (outboundChannel.isActive()) {
            outboundChannel.writeAndFlush(msg).addListener(listener);

            if (msg instanceof ByteBuf) {
                ByteBuf byteBuf = (ByteBuf) msg;
                int size = byteBuf.readableBytes();
                logger.info("Sent {} bytes: {} => {}", size, inboundChannel, outboundChannel);
                if (logger.isDebugEnabled()) {
                    String dump = ByteBufUtil.prettyHexDump(byteBuf, 0, 64);
                    logger.debug("Dump {} => {}: {}", inboundChannel, outboundChannel, dump);
                }
            } else {
                logger.info("Sent {}: {} => {}", msg, inboundChannel, outboundChannel);
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        Promise<Void> promise = ctx.channel().eventLoop().newPromise();
        logger.debug("Closed connection: {}", inboundChannel);
        linker.release(outboundChannel, promise).addListener(f -> {
            if (!f.isSuccess()) {
                logger.error("Release fail: {}", outboundChannel);
                logger.error(f.cause());
            }
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("exception caught {} - {}", inboundChannel, outboundChannel, cause);
        ChannelUtils.closeOnFlush(ctx.channel());
    }
}
