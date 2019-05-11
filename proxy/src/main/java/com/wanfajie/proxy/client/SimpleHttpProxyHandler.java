package com.wanfajie.proxy.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.EmptyByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.base64.Base64;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.net.SocketAddress;
import java.nio.channels.ConnectionPendingException;

public class SimpleHttpProxyHandler extends ChannelOutboundHandlerAdapter {

    private InternalLogger logger = InternalLoggerFactory.getInstance(SimpleHttpProxyHandler.class);

    private final SocketAddress proxyAddress;
    private final String username;
    private final String password;

    private final AsciiString authorization;

    private SocketAddress destinationAddress;
    private boolean enableSsl = false;

    public SimpleHttpProxyHandler(SocketAddress proxyAddress) {
        this.proxyAddress = ObjectUtil.checkNotNull(proxyAddress, "proxyAddress");
        this.username = null;
        this.password = null;
        this.authorization = null;
    }

    public SimpleHttpProxyHandler(SocketAddress proxyAddress, String username, String password) {

        this.proxyAddress = ObjectUtil.checkNotNull(proxyAddress, "proxyAddress");
        this.username = ObjectUtil.checkNotNull(username, "username");
        this.password = ObjectUtil.checkNotNull(password, "password");

        ByteBuf authz = Unpooled.copiedBuffer(username + ':' + password, CharsetUtil.UTF_8);
        ByteBuf authzBase64 = Base64.encode(authz, false);
        this.authorization = new AsciiString("Basic " + authzBase64.toString(CharsetUtil.US_ASCII));

        authz.release();
        authzBase64.release();
    }

    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress,
                        SocketAddress localAddress, ChannelPromise promise) {

        enableSsl = ctx.pipeline().get(SslHandler.class) != null;
        if (enableSsl) {
            logger.warn("HTTP proxy server may not be able to proxy HTTPS protocol properly");
            ctx.pipeline().remove(SslHandler.class);
            logger.warn("Removed SslHandler");
        }

        if (this.destinationAddress != null) {
            promise.setFailure(new ConnectionPendingException());
        } else {
            this.destinationAddress = remoteAddress;
            ctx.connect(this.proxyAddress, localAddress, promise);
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) msg;
            String uri = request.uri();
            String host = request.headers().get(HttpHeaderNames.HOST);

            if (host != null) {
                String scheme = enableSsl ? "https://" : "http://";
                String fullUrl = scheme + host + uri;
                request.setUri(fullUrl);
                logger.debug("HTTP proxy: {} => {}", uri, fullUrl);
            } else {
                logger.warn("Missing host header. [{}]", request);
            }

            if (this.authorization != null) {
                request.headers().set(HttpHeaderNames.PROXY_AUTHORIZATION, this.authorization);
            }

            if (request.headers().contains(HttpHeaderNames.CONNECTION)) {
                request.headers().remove(HttpHeaderNames.CONNECTION);
                request.headers().set(HttpHeaderNames.PROXY_CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            }
        } else if (!(msg instanceof EmptyByteBuf)) {
            logger.info("Unexpected message: {}", msg);

            if (msg instanceof ByteBuf && logger.isDebugEnabled()) {
                String dump = ByteBufUtil.prettyHexDump((ByteBuf) msg, 0, 64);
                logger.debug("Dump: {}", dump);
            }
        }

        ctx.write(msg, promise);
    }

    public final String username() {
        return username;
    }

    public final String password() {
        return password;
    }

    public final SocketAddress proxyAddress() {
        return proxyAddress;
    }

    public final SocketAddress destinationAddress() {
        return destinationAddress;
    }
}
