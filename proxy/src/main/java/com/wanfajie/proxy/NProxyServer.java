package com.wanfajie.proxy;

import java.net.InetSocketAddress;

public class NProxyServer {

    private String localHost;
    private int localPort;

    public NProxyServer(InetSocketAddress localAddress) {
        this.localHost = localAddress.getHostString();
        this.localPort = localAddress.getPort();
    }

    public NProxyServer(String host, int port) {
        this.localHost = host;
        this.localPort = port;
    }
}
