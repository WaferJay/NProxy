package com.wanfajie.nttpclient;

import com.wanfajie.proxy.HttpProxy;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.util.AttributeKey;

import java.util.Map;

public interface HttpRequestParamFlow {
    HttpRequestParamFlow header(CharSequence key, Object value);
    HttpRequestParamFlow header(CharSequence key, Iterable<?> values);
    HttpRequestParamFlow header(HttpHeaders headers);

    HttpRequestParamFlow param(String key, String value);
    HttpRequestParamFlow param(Map<String, String> params);

    HttpRequestParamFlow proxy(HttpProxy proxy);
    HttpRequestParamFlow proxy(HttpProxy.Type type, String host, int port);
}
