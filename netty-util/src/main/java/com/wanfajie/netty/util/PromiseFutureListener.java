package com.wanfajie.netty.util;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;

public class PromiseFutureListener<V> implements GenericFutureListener<Future<V>> {

    private Promise<V> promise;

    public PromiseFutureListener(Promise<V> promise) {
        this.promise = promise;
    }

    @Override
    public void operationComplete(Future<V> future) throws Exception {

        if (future.isSuccess()) {
            V value = future.get();
            promise.setSuccess(value);
        } else {
            promise.setFailure(future.cause());
        }
    }
}
