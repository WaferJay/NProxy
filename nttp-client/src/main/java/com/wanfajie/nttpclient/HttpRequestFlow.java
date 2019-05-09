package com.wanfajie.nttpclient;

import com.wanfajie.proxy.HttpProxy;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public interface HttpRequestFlow extends HttpRequestParamFlow {

    HttpRequestFlow timeout(long delay, TimeUnit unit);
    default HttpRequestFlow timeout(int seconds) {
        return timeout(seconds, TimeUnit.SECONDS);
    }

    HttpRequestFlow onError(Consumer<Throwable> onError);
    HttpRequestFlow onResponse(ResponseConsumer onResponse);

    Future<Void> request();
    Future<Void> request(Promise<Void> promise);

    @Override
    HttpRequestFlow header(CharSequence key, Object value);
    @Override
    HttpRequestFlow header(CharSequence key, Iterable<?> values);
    @Override
    HttpRequestFlow header(HttpHeaders headers);


    @Override
    HttpRequestFlow param(String key, String value);
    @Override
    HttpRequestFlow param(Map<String, String> params);

    @Override
    HttpRequestFlow proxy(HttpProxy proxy);
    @Override
    HttpRequestFlow proxy(HttpProxy.Type type, String host, int port);
}
