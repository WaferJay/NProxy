package com.wanfajie.nttpclient;

import com.wanfajie.nttpclient.exception.InvalidResponseException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.function.Consumer;

public class HttpResponseHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

    private final InternalLogger logger = InternalLoggerFactory.getInstance(HttpResponseHandler.class);

    private final Consumer<Throwable> errorHandler;
    private final ResponseConsumer successHandler;

    public HttpResponseHandler(
            ResponseConsumer successHandler,
            Consumer<Throwable> errorHandler) {

        this.errorHandler = errorHandler;
        this.successHandler = successHandler;
    }

    public Consumer<Throwable> errback() {
        return errorHandler;
    }

    public ResponseConsumer callback() {
        return successHandler;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse response) throws Exception {

        if (response.decoderResult().isSuccess()) {

            if (successHandler != null) {
                successHandler.accept(response);
            }

            if (!HttpUtil.isKeepAlive(response)) {
                ctx.close();
            }
        } else {
            throw new InvalidResponseException(response.toString());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (errorHandler != null) {
            try {
                errorHandler.accept(cause);
            } catch (Exception e) {
                logger.warn("Unhandled exception in error back", e);
            }
        } else {
            ctx.fireExceptionCaught(cause);
        }
    }
}
