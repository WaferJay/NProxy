package com.wanfajie.nproxy.pool;

import com.wanfajie.proxy.HttpProxy;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import io.netty.util.internal.ObjectUtil;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;

public class MemProxyPool implements ProxyPool {

    private final EventLoop eventExecutors;

    private final List<HttpProxy> httpProxies = new LinkedList<>();
    private final List<HttpProxy> httpsProxies = new LinkedList<>();
    private final List<HttpProxy> socksProxies = new LinkedList<>();

    private final Set<Long> addresses = new TreeSet<>();

    public MemProxyPool(EventLoop eventExecutors) {
        this.eventExecutors = eventExecutors;
    }

    private boolean add0(HttpProxy proxy) {
        long address = address2Long(proxy.getAddress());

        List<HttpProxy> proxies = getProxies0(proxy.getType());
        if (addresses.add(address)) {
            return proxies.add(proxy);
        } else {
            return false;
        }
    }

    @Override
    public Future<Boolean> add(HttpProxy proxy) {
        return eventExecutors.submit(() -> add0(proxy));
    }

    @Override
    public Future<Boolean> add(HttpProxy proxy, Promise<Boolean> promise) {
        ObjectUtil.checkNotNull(promise, "promise");

        if (eventExecutors.inEventLoop()) {
            invoke(promise, () -> add0(proxy));
        } else {
            eventExecutors.submit(() -> {
                invoke(promise, () -> add0(proxy));
            });
        }

        return promise;
    }

    private boolean remove0(HttpProxy proxy) {
        long address = address2Long(proxy.getAddress());

        List<HttpProxy> proxies = getProxies0(proxy.getType());
        if (addresses.remove(address)) {
            return proxies.remove(proxy);
        } else {
            return false;
        }
    }

    @Override
    public Future<Boolean> remove(HttpProxy proxy) {
        return eventExecutors.submit(() -> remove0(proxy));
    }

    @Override
    public Future<Boolean> remove(HttpProxy proxy, Promise<Boolean> promise) {

        if (eventExecutors.inEventLoop()) {
            invoke(promise, () -> remove0(proxy));
        } else {
            eventExecutors.submit(() -> {
                invoke(promise, () -> remove0(proxy));
            });
        }

        return promise;
    }

    private HttpProxy get0(int index, HttpProxy.Type type) {
        List<HttpProxy> proxies = getProxies0(type);
        return proxies.get(index);
    }

    @Override
    public Future<HttpProxy> get(int index, HttpProxy.Type type) {
        return eventExecutors.submit(() -> get0(index, type));
    }

    @Override
    public Future<HttpProxy> get(int index, HttpProxy.Type type, Promise<HttpProxy> promise) {
        if (eventExecutors.inEventLoop()) {
            invoke(promise, () -> get0(index, type));
        } else {
            eventExecutors.submit(() -> {
                invoke(promise, () -> get0(index, type));
            });
        }

        return promise;
    }

    @Override
    public Future<List<HttpProxy>> getProxies(HttpProxy.Type type) {
        return eventExecutors.submit(() -> Collections.unmodifiableList(getProxies0(type)));
    }

    @Override
    public Future<List<HttpProxy>> getProxies(HttpProxy.Type type, Promise<List<HttpProxy>> promise) {
        if (eventExecutors.inEventLoop()) {
            invoke(promise, () -> Collections.unmodifiableList(getProxies0(type)));
        } else {
            eventExecutors.submit(() -> {
                invoke(promise, () -> Collections.unmodifiableList(getProxies0(type)));
            });
        }

        return promise;
    }

    private boolean contains0(HttpProxy proxy) {
        return getProxies0(proxy.getType()).contains(proxy);
    }

    @Override
    public Future<Boolean> contains(HttpProxy proxy) {
        return eventExecutors.submit(() -> contains0(proxy));
    }

    @Override
    public Future<Boolean> contains(HttpProxy proxy, Promise<Boolean> promise) {
        if (eventExecutors.inEventLoop()) {
            invoke(promise, () -> contains0(proxy));
        } else {
            eventExecutors.submit(() -> {
                invoke(promise, () -> contains0(proxy));
            });
        }

        return promise;
    }

    private HttpProxy random0(HttpProxy.Type type) {
        List<HttpProxy> proxies = getProxies0(type);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return proxies.get(random.nextInt(0, proxies.size()));
    }

    @Override
    public Future<HttpProxy> random(HttpProxy.Type type) {
        return eventExecutors.submit((Callable<HttpProxy>) this::random0);
    }

    @Override
    public Future<HttpProxy> random(HttpProxy.Type type, Promise<HttpProxy> promise) {
        if (eventExecutors.inEventLoop()) {
            invoke(promise, () -> random0(type));
        } else {
            eventExecutors.submit(() -> {
                invoke(promise, () -> random0(type));
            });
        }

        return promise;
    }

    @Override
    public Future<HttpProxy> random(Promise<HttpProxy> promise) {
        if (eventExecutors.inEventLoop()) {
            invoke(promise, this::random0);
        } else {
            eventExecutors.submit(() -> {
                invoke(promise, this::random0);
            });
        }

        return promise;
    }

    @Override
    public Future<HttpProxy> random() {
        return eventExecutors.submit((Callable<HttpProxy>) this::random0);
    }

    private HttpProxy random0() {

        ThreadLocalRandom random = ThreadLocalRandom.current();

        int httpSize = httpProxies.size();
        int httpsSize = httpsProxies.size();
        int socksSize = socksProxies.size();
        int totalSize = httpSize + httpSize + socksSize;

        int pointer = random.nextInt(0, totalSize);

        if (pointer < httpSize) {
            return httpProxies.get(pointer);
        } else {
            pointer -= httpSize;
        }

        if (pointer < httpsSize) {
            return httpsProxies.get(pointer);
        } else {
            pointer -= httpsSize;
        }

        if (pointer < socksSize) {
            return socksProxies.get(pointer);
        } else {
            throw new IllegalStateException();
        }
    }

    private int size0() {
        return httpProxies.size() + httpsProxies.size() + socksProxies.size();
    }

    @Override
    public Future<Integer> size() {
        return eventExecutors.submit((Callable<Integer>) this::size0);
    }

    @Override
    public Future<Integer> size(Promise<Integer> promise) {
        if (eventExecutors.inEventLoop()) {
            invoke(promise, this::size0);
        } else {
            eventExecutors.submit(() -> invoke(promise, this::size0));
        }

        return promise;
    }

    private int size0(HttpProxy.Type type) {
        return getProxies0(type).size();
    }

    @Override
    public Future<Integer> size(HttpProxy.Type type) {
        return eventExecutors.submit(() -> size0(type));
    }

    @Override
    public Future<Integer> size(HttpProxy.Type type, Promise<Integer> promise) {
        if (eventExecutors.inEventLoop()) {
            promise.setSuccess(size0(type));
            invoke(promise, () -> size0(type));
        } else {
            eventExecutors.submit(() -> {
                invoke(promise, () -> size0(type));
            });
        }

        return promise;
    }

    @Override
    public EventLoop eventLoop() {
        return eventExecutors;
    }

    private List<HttpProxy> getProxies0(HttpProxy.Type type) {
        switch (type) {
            case HTTP:
                return httpProxies;
            case HTTPS:
                return httpsProxies;
            case SOCKS4:
            case SOCKS5:
            case SOCKS:
                return socksProxies;
            default:
                throw new IllegalStateException();
        }
    }

    private long address2Long(InetSocketAddress address) {
        byte[] rawIp = address.getAddress().getAddress();
        int port = address.getPort();
        int ip = (rawIp[0] & 0xff) << 24 |
                 (rawIp[1] & 0xff) << 16 |
                 (rawIp[2] & 0xff) <<  8 |
                 (rawIp[3] & 0xff);

        return (long) ip << 16 | port;
    }

    private static <T> void invoke(Promise<T> promise, Callable<T> callable) {

        T result;
        try {
            result = callable.call();
        } catch (Exception e) {
            promise.setFailure(e);
            return;
        }

        promise.setSuccess(result);
    }
}
