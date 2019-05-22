package com.wanfajie.proxy.server;

import com.wanfajie.netty.util.ChannelUtils;
import com.wanfajie.netty.util.MyByteBufUtil;
import io.netty.buffer.ByteBuf;
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
        logger.info("Client connected {}", inboundChannel);
        listener = new PipelineChannelFutureListener(ctx.channel(), logger);

        Promise<Channel> promise = ctx.channel().eventLoop().newPromise();
        linker.acquire(inboundChannel, promise).addListener(f -> {

            if (f.isSuccess()) {
                outboundChannel = (Channel) f.get();

                logger.debug("Acquired: {} => {}", inboundChannel, outboundChannel);
                inboundChannel.read();
            } else {
                logger.error("Acquire fail {}", inboundChannel, f.cause());
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

            if (logger.isInfoEnabled()) {

                if (msg instanceof ByteBuf) {
                    ByteBuf byteBuf = (ByteBuf) msg;
                    int size = byteBuf.readableBytes();
                    logger.info("Sent {} bytes: {} => {}", size, inboundChannel, outboundChannel);
                    if (logger.isTraceEnabled()) {
                        String dump = MyByteBufUtil.safePrettyHexDump(byteBuf, 0, 128);
                        logger.trace("Dump {} => {}:\n{}", inboundChannel, outboundChannel, dump);
                    }
                } else {
                    logger.info("Sent {}: {} => {}", msg, inboundChannel, outboundChannel);
                }
            }

            outboundChannel.writeAndFlush(msg).addListener(listener);
        } else {

            if (logger.isErrorEnabled()) {
                Object message = msg;
                if (msg instanceof ByteBuf) {
                    message = "\n" + MyByteBufUtil.safePrettyHexDump((ByteBuf) msg, 0, 128);
                }

                logger.error("Received data from [Client: {}]: {}", inboundChannel, message);
            }

            throw new IllegalStateException("Received data at the wrong time.");
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        final Channel remoteChannel = outboundChannel;
        if (remoteChannel == null) {
            return;
        }

        Promise<Void> promise = ctx.channel().eventLoop().newPromise();
        logger.info("Closed connection: {}", inboundChannel);
        linker.release(remoteChannel, promise).addListener(f -> {
            if (!f.isSuccess()) {
                logger.error("Release fail: {}", remoteChannel, f.cause());
            } else {
                logger.debug("Release success {}", remoteChannel);
            }
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("exception caught {} - {}", inboundChannel, outboundChannel, cause);
        ChannelUtils.closeOnFlush(ctx.channel());
    }
}
