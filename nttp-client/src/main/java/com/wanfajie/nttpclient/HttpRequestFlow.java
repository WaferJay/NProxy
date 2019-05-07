package com.wanfajie.nttpclient;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public interface HttpRequestFlow extends HttpRequest {

    HttpRequestFlow timeout(long delay, TimeUnit unit);
    default HttpRequestFlow timeout(int seconds) {
        return timeout(seconds, TimeUnit.SECONDS);
    }

    HttpRequestFlow onError(Consumer<Throwable> onError);
    HttpRequestFlow onResponse(ResponseConsumer onResponse);

    Future<Void> request();
    Future<Void> request(Promise<Void> promise);
}
