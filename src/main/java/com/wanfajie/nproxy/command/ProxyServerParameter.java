package com.wanfajie.nproxy.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(separators = "=")
public class ProxyServerParameter {

    @Parameter(names = {"--proxy-server-host", "-sH"}, order = 20, description = "local binding address")
    private String bindHost = "0.0.0.0";

    @Parameter(names = {"--proxy-http-port", "-pH"}, order = 21, description = "local proxy port for HTTP")
    private int httpProxyPort = 680;

    @Parameter(names = {"--proxy-https-port", "-pL"}, order = 22, description = "local proxy port for HTTPS")
    private int httpsProxyPort = 6443;

    @Parameter(names = {"--proxy-socks-port", "-pS"}, order = 23, description = "local proxy port for SOCKS")
    private int socksProxyPort = 61080;

    public String bindHost() {
        return bindHost;
    }

    public int httpProxyPort() {
        return httpProxyPort;
    }

    public int httpsProxyPort() {
        return httpsProxyPort;
    }

    public int socksProxyPort() {
        return socksProxyPort;
    }
}
