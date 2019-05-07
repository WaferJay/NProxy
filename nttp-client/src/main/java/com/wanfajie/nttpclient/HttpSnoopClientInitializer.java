package com.wanfajie.nttpclient;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

public class HttpSnoopClientInitializer extends ChannelInitializer<SocketChannel> {

    private boolean httpsMode;

    private final Object SSL_CONTEXT_LOCK = new Object();
    private volatile SSLContext sslContext;

    public HttpSnoopClientInitializer() {
        this(false);
    }

    public HttpSnoopClientInitializer(boolean httpsMode) {
        this.httpsMode = httpsMode;
    }

    @Override
    protected void initChannel(SocketChannel channel) throws Exception {
        ChannelPipeline p = channel.pipeline();

        if (httpsMode) {
            SSLEngine sslEngine = sslContext().createSSLEngine();

            sslEngine.setUseClientMode(true);
            p.addLast(new SslHandler(sslEngine));
        }

        p.addLast("http-client-codec", new HttpClientCodec())
         .addLast("http-object-aggregator", new HttpObjectAggregator(512 * 1024))
         .addLast("http-content-decompress", new HttpContentDecompressor());
    }

    private SSLContext sslContext() throws NoSuchAlgorithmException, KeyManagementException {
        if (sslContext == null) {

            synchronized (SSL_CONTEXT_LOCK) {
                if (sslContext == null) {
                    sslContext = SSLContext.getInstance("TLS");
                    sslContext.init(null, null, null);
                }
            }
        }

        return sslContext;
    }
}
