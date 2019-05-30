package com.wanfajie.nproxy.pool;

import com.wanfajie.proxy.HttpProxy;
import com.wanfajie.proxy.HttpProxySupplier;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public interface ProxyPool {

    Future<Boolean> add(HttpProxy proxy);
    Future<Boolean> add(HttpProxy proxy, Promise<Boolean> promise);
    Future<Boolean> remove(HttpProxy proxy);
    Future<Boolean> remove(HttpProxy proxy, Promise<Boolean> promise);

    Future<HttpProxy> get(int index, HttpProxy.Type type);
    Future<HttpProxy> get(int index, HttpProxy.Type type, Promise<HttpProxy> promise);

    Future<List<HttpProxy>> getProxies(HttpProxy.Type type);
    Future<List<HttpProxy>> getProxies(HttpProxy.Type type, Promise<List<HttpProxy>> promise);

    Future<Boolean> contains(HttpProxy proxy);
    Future<Boolean> contains(HttpProxy proxy, Promise<Boolean> promise);

    Future<HttpProxy> random();
    Future<HttpProxy> random(Promise<HttpProxy> promise);
    Future<HttpProxy> random(HttpProxy.Type type);
    Future<HttpProxy> random(HttpProxy.Type type, Promise<HttpProxy> promise);

    Future<Integer> size();
    Future<Integer> size(Promise<Integer> promise);
    Future<Integer> size(HttpProxy.Type type);
    Future<Integer> size(HttpProxy.Type type, Promise<Integer> promise);
    EventLoop eventLoop();

    default HttpProxySupplier supplier(HttpProxy.Type type) {
        return new DefaultPoolProxySupplier(this, type);
    }

    class DefaultPoolProxySupplier implements HttpProxySupplier {

        private final AtomicInteger index = new AtomicInteger();
        private final ProxyPool proxyPool;
        private final HttpProxy.Type type;

        public DefaultPoolProxySupplier(ProxyPool pool, HttpProxy.Type type) {
            this.proxyPool = pool;
            this.type = type;
        }

        @Override
        public Future<HttpProxy> get(Promise<HttpProxy> promise) {

            proxyPool.size(type).addListener(f -> {
                int size = (Integer) f.get();

                if (size == 0) {
                    Exception e = new IndexOutOfBoundsException("Index: " + index.get() + ", Size: " + size);
                    promise.setFailure(e);
                } else {
                    int i = index.getAndIncrement();
                    proxyPool.get(i % size, type, promise);
                }
            });

            return promise;
        }

        @Override
        public Future<HttpProxy> get() {
            return get(proxyPool.eventLoop().newPromise());
        }
    }
}
