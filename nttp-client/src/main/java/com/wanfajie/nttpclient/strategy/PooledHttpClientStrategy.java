package com.wanfajie.nttpclient.strategy;

import com.wanfajie.nttpclient.HttpSnoopClientInitializer;
import com.wanfajie.proxy.HttpProxy;
import com.wanfajie.proxy.client.HttpProxyInitializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.pool.AbstractChannelPoolMap;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.channel.socket.SocketChannel;
import io.netty.resolver.AddressResolver;
import io.netty.resolver.DefaultAddressResolverGroup;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.net.InetSocketAddress;
import java.util.Objects;

public class PooledHttpClientStrategy implements HttpClientStrategy {

    private static final AttributeKey<HttpProxy> PROXY_KEY = HttpProxyInitializer.PROXY_KEY;
    private static final AttributeKey<ProxyAddressWrapper> PROXY_WRAPPER_KEY = AttributeKey.newInstance("proxyAddressWrapper");
    private static final int DEFAULT_MAX_CONNECTIONS = 2;

    private static final DefaultAddressResolverGroup RESOLVER = DefaultAddressResolverGroup.INSTANCE;
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(PooledHttpClientStrategy.class);

    private final Bootstrap bootstrap;
    private final EventLoopGroup workers;
    private final ChannelInitializer<? extends SocketChannel> initializer;

    private final int maxConnection;

    private AbstractChannelPoolMap<ProxyAddressWrapper, FixedChannelPool> channelPoolMap;

    public PooledHttpClientStrategy(Bootstrap bootstrap, EventLoopGroup workers,
                                    int maxConnection, boolean httpsMode) {

        ObjectUtil.checkNotNull(bootstrap, "bootstrap");
        ObjectUtil.checkNotNull(workers, "workers");
        ObjectUtil.checkPositiveOrZero(maxConnection, "maxConnection");

        this.bootstrap = bootstrap.clone().option(ChannelOption.SO_KEEPALIVE, true);
        this.workers = workers;
        this.maxConnection = maxConnection == 0 ? DEFAULT_MAX_CONNECTIONS : maxConnection;
        ChannelInitializer<SocketChannel> httpInitializer = new HttpSnoopClientInitializer(httpsMode);
        this.initializer = new HttpProxyInitializer(httpInitializer);
        initChannelPool();
    }

    public PooledHttpClientStrategy(EventLoopGroup worker, Class<? extends SocketChannel> channelClass, int maxConnection) {
        this(
            new Bootstrap()
                .group(worker)
                .channel(channelClass),
            worker,
            maxConnection,
            false
        );
    }

    private void initChannelPool() {
        ChannelPoolHandler handler = new ChannelPoolHandler() {
            @Override
            public void channelReleased(Channel channel) {
                logger.debug("Released: {}", channel);
                if (!channel.isActive()) {
                    logger.debug("Channel {} is closed when it is released", channel);
                }
            }

            @Override
            public void channelAcquired(Channel channel) {
                logger.debug("Acquired: {}", channel);
            }

            @Override
            public void channelCreated(Channel channel) {
                channel.pipeline().addLast(initializer);
                logger.debug("Created: {}", channel);
            }
        };

        channelPoolMap = new AbstractChannelPoolMap<ProxyAddressWrapper, FixedChannelPool>() {
            @Override
            protected FixedChannelPool newPool(ProxyAddressWrapper wrapper) {
                Bootstrap b = bootstrap.clone()
                        .attr(PROXY_KEY, wrapper.proxy)
                        .attr(PROXY_WRAPPER_KEY, wrapper)
                        .remoteAddress(wrapper.address);

                FixedChannelPool pool = new FixedChannelPool(b, handler, maxConnection);
                logger.debug("new pool {} for {}", pool, wrapper);
                return pool;
            }
        };
    }

    private static Future<InetSocketAddress> doResolver(EventExecutor executor, InetSocketAddress address) {
        AddressResolver<InetSocketAddress> resolver = RESOLVER.getResolver(executor);
        Promise<InetSocketAddress> promise = executor.newPromise();
        if (resolver.isResolved(address)) {
            promise.setSuccess(address);
        } else {
            resolver.resolve(address, promise);
        }
        return promise;
    }

    private Future<Channel> acquire(Future<ProxyAddressWrapper> future, Promise<Channel> promise) {

        if (future.isDone()) {
            if (future.isSuccess()) {
                ProxyAddressWrapper wrapper = future.getNow();

                return channelPoolMap.get(wrapper)
                        .acquire(promise);
            }

            promise.setFailure(future.cause());
            return promise;
        }

        future.addListener(f -> {
            if (f.isSuccess()) {
                ProxyAddressWrapper w = (ProxyAddressWrapper) f.get();

                channelPoolMap.get(w)
                    .acquire(promise);
            } else {
                promise.setFailure(f.cause());
            }
        });

        return promise;
    }

    private Future<ProxyAddressWrapper> createResolvedWrapper(InetSocketAddress address, HttpProxy proxy) {
        Promise<ProxyAddressWrapper> promise = workers.next().newPromise();
        Future<InetSocketAddress> future = doResolver(workers.next(), address);

        if (future.isDone()) {
            if (future.isSuccess()) {
                promise.setSuccess(new ProxyAddressWrapper(future.getNow(), proxy));
            } else {
                promise.setFailure(future.cause());
            }

            return promise;
        }

        future.addListener(f -> {
            if (f.isSuccess()) {
                promise.setSuccess(new ProxyAddressWrapper((InetSocketAddress) f.get(), proxy));
            } else {
                promise.setFailure(f.cause());
            }
        });
        return promise;
    }

    @Override
    public boolean keepAlive() {
        return true;
    }

    @Override
    public Future<Channel> createChannel(InetSocketAddress address, HttpProxy proxy) {
        ObjectUtil.checkNotNull(address, "address");
        Future<ProxyAddressWrapper> future =  createResolvedWrapper(address, proxy);
        return acquire(future, workers.next().newPromise());
    }

    @Override
    public Future<Void> recycler(Channel channel) {
        return channelPoolMap.get(channel.attr(PROXY_WRAPPER_KEY).get())
                .release(channel)
                .addListener(f -> {
                    if (!f.isSuccess()) {
                        logger.warn("Channel recycle fail", f.cause());
                    }
                });
    }

    @Override
    public void close() {
        channelPoolMap.close();
    }

    /**
     * {@link PooledHttpClientStrategy#channelPoolMap} 的键，整合目标地址和代理
     *
     * @see PooledHttpClientStrategy#initChannelPool()
     * @see #equals(Object)
     */
    private static final class ProxyAddressWrapper {
        private final InetSocketAddress address;
        private final HttpProxy proxy;

        private ProxyAddressWrapper(InetSocketAddress address, HttpProxy proxy) {
            this.address = address;
            this.proxy = proxy;
        }

        /**
         * 相同HTTP代理将会相等，其他情况只有相同的{@link #proxy}和{@link #address}才相等
         * @see #hashCode() 情况类似
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ProxyAddressWrapper that = (ProxyAddressWrapper) o;
            if (!Objects.equals(proxy, that.proxy)) return false;

            if (proxy == null) {
                // 代理都是null则比较address
                return Objects.equals(address, that.address);
            } else {
                switch (proxy.getType()) {
                case HTTP:
                    // HTTP代理每次请求包含完整URL, 它是可以重用的
                    return true;
                default:
                    // 其他协议代理存在状态切换 eg: CONNECT, 所以判断是否全部相同
                    return Objects.equals(address, that.address);
                }
            }
        }

        @Override
        public int hashCode() {
            if (proxy != null) {
                switch (proxy.getType()) {
                case HTTP:
                    return proxy.getAddress().hashCode();
                default:
                    return Objects.hash(proxy, address);
                }
            } else {
                return address.hashCode();
            }
        }

        @Override
        public String toString() {
            return "[" +
                    "address=" + address +
                    ", proxy=" + proxy +
                    ']';
        }
    }
}
