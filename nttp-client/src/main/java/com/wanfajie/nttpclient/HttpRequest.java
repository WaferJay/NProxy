package com.wanfajie.nttpclient;

import com.wanfajie.proxy.HttpProxy;
import io.netty.handler.codec.http.HttpHeaders;

import java.util.Map;

public interface HttpRequest {
    HttpRequestFlow header(CharSequence key, Object value);
    HttpRequestFlow header(CharSequence key, Iterable<?> values);
    HttpRequestFlow header(HttpHeaders headers);

    HttpRequestFlow param(String key, String value);
    HttpRequestFlow param(Map<String, String> params);

    HttpRequestFlow proxy(HttpProxy proxy);
    HttpRequestFlow proxy(HttpProxy.Type type, String host, int port);
}
