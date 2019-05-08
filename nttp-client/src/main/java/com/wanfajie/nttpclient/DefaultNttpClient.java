package com.wanfajie.nttpclient;

import com.wanfajie.nttpclient.config.NttpClientConfig;
import com.wanfajie.nttpclient.exception.SchemaException;
import com.wanfajie.nttpclient.strategy.DefaultHttpClientStrategyFactory;
import com.wanfajie.nttpclient.strategy.HttpClientStrategy;
import com.wanfajie.nttpclient.strategy.HttpClientStrategyFactory;
import io.netty.handler.codec.http.FullHttpRequest;

import java.net.InetSocketAddress;

public class DefaultNttpClient implements NttpClient {

    private final HttpClientStrategy httpStrategy;
    private final HttpClientStrategy httpsStrategy;

    public DefaultNttpClient(NttpClientConfig config) {
        this(config, null);
    }

    public DefaultNttpClient(NttpClientConfig config, HttpClientStrategyFactory strategyFactory) {
        if (strategyFactory == null) {
            strategyFactory = new DefaultHttpClientStrategyFactory();
        }

        this.httpStrategy = strategyFactory.create(config, false);
        this.httpsStrategy = strategyFactory.create(config, true);
    }

    @Override
    public HttpRequestFlow send(InetSocketAddress address, String schema, FullHttpRequest request) {
        schema = schema.toUpperCase();
        HttpClientStrategy strategy;
        if ("HTTP".equals(schema)) {
            strategy = httpStrategy;
        } else if ("HTTPS".equals(schema)) {
            strategy = httpsStrategy;
        } else {
            throw new SchemaException(schema);
        }

        return new DefaultHttpRequestFlow(strategy, address, request);
    }

    @Override
    public void close() {
        httpStrategy.close();
        httpsStrategy.close();
    }
}
