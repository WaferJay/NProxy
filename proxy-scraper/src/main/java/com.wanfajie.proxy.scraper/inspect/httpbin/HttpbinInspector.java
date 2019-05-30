package com.wanfajie.proxy.scraper.inspect.httpbin;

import com.wanfajie.nttpclient.NttpClient;
import com.wanfajie.proxy.scraper.inspect.AbstractInspector;
import com.wanfajie.proxy.scraper.inspect.InspectorChannelHandler;

import java.net.URI;

public class HttpbinInspector extends AbstractInspector {

    private static final URI HTTP_BIN_URL = URI.create("http://httpbin.org/ip");

    public HttpbinInspector(NttpClient.Builder builder, int concurrent) {
        super(builder, concurrent);
    }

    @Override
    protected InspectorChannelHandler createInspectorChannelHandler() {
        return new HttpbinInspectorChannelHandler();
    }

    @Override
    protected URI inspectorURI() {
        return HTTP_BIN_URL;
    }
}
