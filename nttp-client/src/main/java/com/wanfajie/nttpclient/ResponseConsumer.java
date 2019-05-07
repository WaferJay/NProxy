package com.wanfajie.nttpclient;

import io.netty.handler.codec.http.FullHttpResponse;

public interface ResponseConsumer {
    void accept(FullHttpResponse response) throws Exception;
}
