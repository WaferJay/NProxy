package com.wanfajie.nttpclient;

import com.wanfajie.netty.util.HttpUtils;
import com.wanfajie.nttpclient.exception.AlreadyCalledException;
import com.wanfajie.nttpclient.strategy.HttpClientStrategy;
import com.wanfajie.proxy.HttpProxy;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.StringUtil;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class DefaultHttpRequestFlow implements HttpRequestFlow {

    private final Object CALLED_LOCK = new Object();
    private volatile boolean called = false;

    private final InetSocketAddress socketAddress;
    private final HttpRequest request;
    private final HttpClientStrategy strategy;

    private Consumer<Throwable> errorHandler;
    private ResponseConsumer responseHandler;

    private final Map<String, String> paramsMap = new HashMap<>();

    private long delay = 0;
    private TimeUnit unit = TimeUnit.SECONDS;

    private HttpProxy proxy;

    DefaultHttpRequestFlow(HttpClientStrategy strategy, InetSocketAddress address, HttpRequest request) {
        this.strategy = strategy;
        this.request = request;
        this.socketAddress = address;
    }

    @Override
    public DefaultHttpRequestFlow onError(Consumer<Throwable> onError) {
        errorHandler = onError;
        return this;
    }

    @Override
    public DefaultHttpRequestFlow onResponse(ResponseConsumer onResponse) {
        responseHandler = onResponse;
        return this;
    }

    @Override
    public HttpRequestFlow header(CharSequence key, Object value) {
        request.headers().set(key, value);
        return this;
    }

    @Override
    public HttpRequestFlow header(CharSequence key, Iterable<?> values) {
        request.headers().set(key, values);
        return this;
    }

    @Override
    public HttpRequestFlow header(HttpHeaders headers) {
        request.headers().setAll(headers);
        return this;
    }

    @Override
    public HttpRequestFlow param(String key, String value) {
        paramsMap.put(key, value);
        return this;
    }

    @Override
    public HttpRequestFlow param(Map<String, String> params) {
        paramsMap.putAll(params);
        return this;
    }

    @Override
    public HttpRequestFlow timeout(long delay, TimeUnit unit) {
        this.delay = ObjectUtil.checkPositiveOrZero(delay, "delay");
        this.unit = unit;
        return this;
    }

    @Override
    public HttpRequestFlow proxy(HttpProxy proxy) {
        this.proxy = proxy;
        return this;
    }

    @Override
    public HttpRequestFlow proxy(HttpProxy.Type type, String host, int port) {
        this.proxy = new HttpProxy(type, host, port);
        return this;
    }

    private void joinParams() {
        URI uri = URI.create(request.uri());
        String path = uri.getRawPath();
        String query = uri.getRawQuery();
        String fragment = uri.getRawFragment();

        StringBuilder sb = new StringBuilder(256);
        sb.append(path);

        boolean isEmptyQuery = StringUtil.isNullOrEmpty(query);
        boolean isEmptyParams = paramsMap.isEmpty();
        if (!isEmptyQuery || !isEmptyParams) {
            sb.append("?");
        }

        if (!isEmptyQuery) {
            sb.append(query);

            if (!isEmptyParams) {
                sb.append("&");
            }
        }

        HttpUtils.urlencode(sb, paramsMap);

        if (!StringUtil.isNullOrEmpty(fragment)) {
            sb.append("#");
            sb.append(fragment);
        }

        request.setUri(sb.toString());
    }

    private void recyclerChannel(Channel ch) {
        if (ch.isActive()) {
            ch.pipeline().remove(HttpResponseHandler.class);

            boolean hasTimeoutHandler = ch.pipeline().get(ReadTimeoutHandler.class) != null;
            if (hasTimeoutHandler) {
                ch.pipeline().remove(ReadTimeoutHandler.class);
            }
        }

        strategy.recycler(ch);
    }

    private void addTimeoutHandler(Channel ch) {
        if (delay <= 0) {
            return;
        }

        ch.pipeline()
          .addBefore("resp-handler", "read-timeout-handler",
                  new ReadTimeoutHandler(delay, unit));
    }

    private void checkCalled() {
        if (called) {
            throw new AlreadyCalledException();
        }

        synchronized (CALLED_LOCK) {

            if (called) {
                throw new AlreadyCalledException();
            } else {
                called = true;
            }
        }
    }

    @Override
    public Future<Void> request() {
        DefaultPromise<Void> promise = new DefaultPromise<>(GlobalEventExecutor.INSTANCE);
        return request(promise);
    }

    private Consumer<Throwable> createErrorBack(Promise<Void> promise) {

        final Consumer<Throwable> errorHandler = this.errorHandler;

        return e -> {
            if (errorHandler != null) {
                try {
                    errorHandler.accept(e);
                } catch (Exception e1) {
                    e1.addSuppressed(e);
                    promise.setFailure(e1);
                    throw e1;
                }
            }
            promise.tryFailure(e);
        };
    }

    private ResponseConsumer createResponseBack(Promise<Void> promise) {

        final ResponseConsumer responseHandler = this.responseHandler;

        return resp -> {
            if (responseHandler != null) {
                responseHandler.accept(resp);
            }
            promise.setSuccess(null);
        };
    }

    @Override
    public Future<Void> request(Promise<Void> promise) {
        checkCalled();
        joinParams();

        Future<Channel> future = strategy.createChannel(socketAddress, proxy);

        Consumer<Throwable> eb = createErrorBack(promise);
        ResponseConsumer cb = createResponseBack(promise);

        future.addListener(f -> {
            if (!f.isSuccess()) {
                eb.accept(f.cause());
                return;
            }

            Channel ch = future.get();
            promise.addListener(ff -> recyclerChannel(ch));

            HttpResponseHandler handler = new HttpResponseHandler(cb, eb);
            ch.pipeline()
                    .addLast("resp-handler", handler);

            addTimeoutHandler(ch);

            if (strategy.keepAlive()) {
                request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            } else {
                request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
            }

            // 错误在管道中传递，最终被HttpResponseHandler处理，这里就不注册监听器了
            ch.writeAndFlush(request);
        });

        return promise;
    }
}
