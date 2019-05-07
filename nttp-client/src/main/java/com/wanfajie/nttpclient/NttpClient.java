package com.wanfajie.nttpclient;

import com.wanfajie.nttpclient.exception.SchemaException;
import io.netty.buffer.Unpooled;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.internal.StringUtil;

import java.io.Closeable;
import java.net.InetSocketAddress;
import java.net.URI;

public interface NttpClient extends Closeable, AutoCloseable {

    HttpRequestFlow send(InetSocketAddress address, String schema, FullHttpRequest request);

    default HttpRequestFlow send(String method, URI uri) {
        String host = uri.getHost();
        int port = uri.getPort();
        String schema = uri.getScheme();
        if (port == -1) {
            if ("HTTP".equalsIgnoreCase(schema)) {
                port = 80;
            } else if ("HTTPS".equalsIgnoreCase(schema)) {
                port = 443;
            } else {
                throw new SchemaException(schema);
            }
        }

        String uriStr = uri.getRawPath();
        if (!StringUtil.isNullOrEmpty(uri.getRawQuery())) {
            uriStr += "?" + uri.getRawQuery();
        }

        FullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.valueOf(method.toUpperCase()),
                uriStr, Unpooled.EMPTY_BUFFER);

        if (uri.getPort() == -1) {
            request.headers().set("Host", host);
        } else {
            request.headers().set("Host", host + ":" + port);
        }

        InetSocketAddress address = InetSocketAddress.createUnresolved(host, port);
        return send(address, schema, request);
    }

    default HttpRequestFlow get(String url) {
        return send("GET", URI.create(url));
    }

    default HttpRequestFlow get(URI url) {
        return send("GET", url);
    }

    class Builder {

        private EventLoopGroup worker;
        private Class<? extends SocketChannel> channelClass;
        private int maxConnectionPoolSize = 0;
        private int connectTimeout = 0;

        public Builder group(EventLoopGroup worker) {
            this.worker = worker;
            return this;
        }

        public Builder channel(Class<? extends SocketChannel> channelClass) {
            this.channelClass = channelClass;
            return this;
        }

        public Builder maxConnectionPerServer(int size) {
            this.maxConnectionPoolSize = size;
            return this;
        }

        public Builder connectTimeout(int seconds) {
            this.connectTimeout = seconds;
            return this;
        }

        public NttpClient build() {
            return new DefaultNttpClient(worker, channelClass, maxConnectionPoolSize, connectTimeout);
        }
    }
}
