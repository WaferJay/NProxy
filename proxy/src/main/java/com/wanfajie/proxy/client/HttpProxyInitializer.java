package com.wanfajie.proxy.client;

import com.wanfajie.proxy.HttpProxy;
import com.wanfajie.proxy.ProxyAuth;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.proxy.Socks4ProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import java.net.SocketAddress;

public class HttpProxyInitializer extends ChannelInitializer<SocketChannel> {

    public static final AttributeKey<HttpProxy> PROXY_KEY = AttributeKey.newInstance("proxy");
    private static final String PROXY_HANDLER_NAME = "proxy-handler";

    private ChannelInitializer<? extends SocketChannel> initializer;

    public HttpProxyInitializer(ChannelInitializer<? extends SocketChannel> initializer) {
        this.initializer = initializer;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ch.pipeline().addLast(initializer);

        if (ch.hasAttr(PROXY_KEY)) {
            Attribute<HttpProxy> proxyValue = ch.attr(PROXY_KEY);
            HttpProxy proxy = proxyValue.get();

            ChannelHandler handler = createProxyHandler(proxy);
            if (proxy.getType() == HttpProxy.Type.HTTP) {
                ch.pipeline().addAfter("http-client-codec", PROXY_HANDLER_NAME, handler);
            } else {
                ch.pipeline().addFirst(PROXY_HANDLER_NAME, handler);
            }
        }
    }

    private static ChannelHandler createProxyHandler(HttpProxy proxy) {
        ProxyAuth auth = proxy.getAuth();

        String user = null;
        String pwd = null;
        if (auth != null) {
            user = auth.getUser();
            pwd = auth.getPassword();
        }
        SocketAddress address = proxy.getAddress();

        switch (proxy.getType()) {
        case HTTP:
            if (user == null || pwd == null) {
                return new SimpleHttpProxyHandler(address);
            } else {
                return new SimpleHttpProxyHandler(address, user, pwd);
            }
        case HTTPS:
            if (user == null || pwd == null) {
                return new HttpProxyHandler(address);
            } else {
                return new HttpProxyHandler(address, user, pwd);
            }
        case SOCKS:
        case SOCKS5:
            return new Socks5ProxyHandler(address, user, pwd);
        case SOCKS4:
            return new Socks4ProxyHandler(address, user);
        default:
            throw new IllegalStateException();
        }
    }
}
