package com.wanfajie.proxy.scraper.inspect;

import com.wanfajie.netty.util.MyByteBufUtil;
import com.wanfajie.proxy.HttpProxy;
import com.wanfajie.proxy.client.HttpProxyInitializer;
import com.wanfajie.proxy.scraper.inspect.httpbin.HttpbinInspectorChannelHandler;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.net.SocketAddress;

public abstract class InspectorChannelHandler extends ChannelDuplexHandler implements ReportGenerator {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(HttpbinInspectorChannelHandler.class);

    private long connectingTimestamp;
    private long connectedTimestamp;
    private long receivedTimestamp;
    private boolean isAnonymous = false;

    private HttpProxy proxy;

    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
        proxy = ctx.channel().attr(HttpProxyInitializer.PROXY_KEY).get();
        if (proxy == null) {
            throw new IllegalStateException();
        }

        recordConnecting();
        ctx.connect(remoteAddress, localAddress, promise);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        recordConnected();
        ctx.fireChannelActive();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof FullHttpResponse) {
            FullHttpResponse response = (FullHttpResponse) msg;
            int readerIndex = response.content().readerIndex();
            try {
                recordReceived();
                isAnonymous = isAnonymous(response);
            } catch (Exception e) {
                logger.info("Decode fail", e);

                if (logger.isDebugEnabled()) {
                    logger.debug("Dump: {}", MyByteBufUtil.safePrettyHexDump(response.content(), 0, 128));
                }

                ReferenceCountUtil.release(msg);
                throw e;
            }

            response.content().readerIndex(readerIndex);
        }

        ctx.fireChannelRead(msg);
    }

    // TODO: 验证Response内容的方法

    protected abstract boolean isAnonymous(FullHttpResponse response);

    protected long recordConnecting() {
        connectingTimestamp = System.currentTimeMillis();
        return connectingTimestamp;
    }

    protected long recordConnected() {
        connectedTimestamp = System.currentTimeMillis();
        return connectedTimestamp;
    }

    protected long recordReceived() {
        receivedTimestamp = System.currentTimeMillis();
        return receivedTimestamp;
    }

    public final long getConnectingTimestamp() {
        return connectingTimestamp;
    }

    public final long getConnectedTimestamp() {
        return connectedTimestamp;
    }

    public final long getReceivedTimestamp() {
        return receivedTimestamp;
    }

    @Override
    public final HttpProxy proxy() {
        return proxy;
    }

    @Override
    public EvaluationReport createReport() {

        int totalDuration = (int) (receivedTimestamp - connectingTimestamp);
        int connectDuration =  (int) (connectedTimestamp - connectingTimestamp);
        int transportDuration = (int) (receivedTimestamp - connectedTimestamp);

        return new EvaluationReport(proxy, isAnonymous, totalDuration,
                connectDuration, transportDuration, receivedTimestamp);
    }
}
