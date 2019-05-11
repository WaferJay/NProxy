package com.wanfajie.proxy.scraper.inspect;

import com.wanfajie.nttpclient.NttpClient;
import com.wanfajie.nttpclient.config.NttpClientConfig;
import com.wanfajie.nttpclient.strategy.HttpClientStrategy;
import com.wanfajie.nttpclient.strategy.HttpClientStrategyFactory;
import com.wanfajie.nttpclient.strategy.SimpleHttpClientStrategy;
import com.wanfajie.proxy.HttpProxy;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import io.netty.util.internal.PlatformDependent;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

public abstract class AbstractInspector implements Inspector, HttpClientStrategyFactory {

    private static final AttributeKey<ReportGenerator> REPORT_GENERATOR_KEY = AttributeKey.newInstance("inspectReportGenerator");

    private final NttpClient client;

    private ConcurrentMap<HttpProxy, Promise<EvaluationReport>> tasks = PlatformDependent.newConcurrentHashMap();
    private InspectorHttpClientStrategy httpClientStrategy;
    private InspectorHttpClientStrategy httpsClientStrategy;
    private volatile boolean closed = false;

    public AbstractInspector(NttpClient.Builder builder) {
        this.client = builder.strategyFactory(this).build();
    }

    protected abstract InspectorChannelHandler createInspectorChannelHandler();
    protected abstract URI inspectorURI();

    @Override
    public Future<EvaluationReport> inspect(HttpProxy proxy, Promise<EvaluationReport> promise) {
        if (closed) {
            promise.setFailure(new InspectorClosedException());
            return promise;
        }

        tasks.put(proxy, promise);
        promise.addListener(f -> tasks.remove(proxy));

        client.get(inspectorURI())
                .onError(promise::setFailure)
                .proxy(proxy)
                .request()
                .addListener(f -> {
                    if (!f.isSuccess()) {
                        promise.tryFailure(f.cause());
                    }
                });

        return promise;
    }

    private void generateReport(ReportGenerator generator) {
        Promise<EvaluationReport> promise = tasks.get(generator.proxy());

        if (promise != null && !promise.isDone()) {
            promise.trySuccess(generator.createReport());
        }
    }

    @Override
    public final synchronized HttpClientStrategy create(NttpClientConfig config, boolean httpsMode) {
        InspectorHttpClientStrategy strategy;
        if (httpsMode) {
            strategy = httpsClientStrategy;
        } else {
            strategy = httpClientStrategy;
        }

        if (strategy == null) {
            Bootstrap bs = createBootstrap(config);
            strategy = new InspectorHttpClientStrategy(bs, httpsMode);
        }

        if (httpsMode) {
            httpsClientStrategy = strategy;
        } else {
            httpClientStrategy = strategy;
        }

        return strategy;
    }

    private static Bootstrap createBootstrap(NttpClientConfig config) {

        EventLoopGroup group = config.group();
        int connectTimeoutSeconds = config.connectTimeout();

        Bootstrap bs = new Bootstrap()
                .attr(REPORT_GENERATOR_KEY, null)
                .option(ChannelOption.SO_KEEPALIVE, false)
                .group(group)
                .channel(config.channelClass());

        if (connectTimeoutSeconds > 0) {
            bs.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutSeconds * 1000);
        }

        return bs;
    }

    protected class InspectorHttpClientStrategy extends SimpleHttpClientStrategy {

        InspectorHttpClientStrategy(Bootstrap bootstrap, boolean httpsMode) {
            super(bootstrap, httpsMode);
        }

        @Override
        protected ChannelInitializer<SocketChannel> createInitializer() {
            ChannelInitializer<SocketChannel> initializer = super.createInitializer();
            return new InspectorInitializer(initializer);
        }

        @Override
        public Future<Void> recycler(Channel channel) {
            ReportGenerator generator = channel.attr(REPORT_GENERATOR_KEY).get();
            generateReport(generator);
            return super.recycler(channel);
        }
    }

    private class InspectorInitializer extends ChannelInitializer<SocketChannel> {

        private ChannelInitializer<SocketChannel> initializer;

        private InspectorInitializer(ChannelInitializer<SocketChannel> initializer) {
            this.initializer = initializer;
        }

        @Override
        protected void initChannel(SocketChannel ch) {
            InspectorChannelHandler handler = createInspectorChannelHandler();

            ch.pipeline()
                    .addLast(initializer)
                    .addLast(handler);

            ch.attr(REPORT_GENERATOR_KEY).set(handler);
        }
    }

    @Override
    public void close() {
        closed = true;
        try {
            client.close();
        } catch (IOException ignored) {
        }

        Map<HttpProxy, Promise<EvaluationReport>> tasks = this.tasks;
        this.tasks = null;

        for (Map.Entry<HttpProxy, Promise<EvaluationReport>> entry : tasks.entrySet()) {

            if (!entry.getValue().isDone()) {
                entry.getValue().tryFailure(new InspectorClosedException());
            }
        }

        tasks.clear();
    }
}
