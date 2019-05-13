package com.wanfajie.proxy;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

public interface HttpProxySupplier {
    Future<HttpProxy> get(Promise<HttpProxy> promise);

    Future<HttpProxy> get();
}
