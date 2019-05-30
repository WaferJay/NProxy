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
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.SynchronousQueue;

public abstract class AbstractInspector implements Inspector, HttpClientStrategyFactory {

    private static final AttributeKey<ReportGenerator> REPORT_GENERATOR_KEY = AttributeKey.newInstance("inspectReportGenerator");

    private final InternalLogger logger = InternalLoggerFactory.getInstance(this.getClass());

    private final NttpClient client;

    private final ConcurrentMap<HttpProxy, Promise<EvaluationReport>> runningTasks = PlatformDependent.newConcurrentHashMap();
    private final BlockingQueue<Task> tasksQueue = new SynchronousQueue<>();
    private final int concurrent;

    private InspectorHttpClientStrategy httpClientStrategy;
    private InspectorHttpClientStrategy httpsClientStrategy;
    private volatile boolean closed = false;

    private TakeQueueThread takeQueueThread = new TakeQueueThread();

    public AbstractInspector(NttpClient.Builder builder, int concurrent) {
        this.client = builder.strategyFactory(this).build();
        this.concurrent = concurrent;

        takeQueueThread.start();
    }

    protected abstract InspectorChannelHandler createInspectorChannelHandler();
    protected abstract URI inspectorURI();

    @Override
    public Future<EvaluationReport> inspect(HttpProxy proxy, Promise<EvaluationReport> promise) {
        if (closed) {
            promise.setFailure(new InspectorClosedException());
            return promise;
        }

        tasksQueue.offer(new Task(proxy, promise));

        return promise;
    }

    private void inspect0(HttpProxy proxy, Promise<EvaluationReport> promise) {

        runningTasks.put(proxy, promise);
        promise.addListener(f -> {

            runningTasks.remove(proxy);
            takeQueueThread.wakeUp();

            if (f.isSuccess()) {
                logger.debug("Complete inspection {} [{}]", proxy, f.get());
            } else {
                logger.debug("Inspection failure {}", proxy);
                if (logger.isTraceEnabled()) {
                    logger.trace("Inspection error processing {}", proxy, f.cause());
                }
            }
        });

        client.get(inspectorURI())
                .onError(promise::tryFailure)
                .proxy(proxy)
                .request();
    }

    private void generateReport(ReportGenerator generator) {
        Promise<EvaluationReport> promise = runningTasks.get(generator.proxy());

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
        takeQueueThread.interrupt();

        try {
            client.close();
        } catch (IOException ignored) {
        }

        for (Map.Entry<HttpProxy, Promise<EvaluationReport>> entry : runningTasks.entrySet()) {

            if (!entry.getValue().isDone()) {
                entry.getValue().tryFailure(new InspectorClosedException());
            }
        }
        runningTasks.clear();

        for (Task task : tasksQueue) {
            task.promise.setFailure(new InspectorClosedException());
        }
        tasksQueue.clear();
    }

    private static class Task {
        private HttpProxy proxy;
        private Promise<EvaluationReport> promise;

        private Task(HttpProxy proxy, Promise<EvaluationReport> promise) {
            this.proxy = proxy;
            this.promise = promise;
        }
    }

    private class TakeQueueThread extends Thread {

        private final InternalLogger logger = InternalLoggerFactory.getInstance(this.getClass());
        private final Object signal = new Object();

        private TakeQueueThread() {
            String outerName = AbstractInspector.this.getClass().getTypeName();
            setName(outerName + "-takeQueue");
        }

        @Override
        public void run() {

            while (!closed) {

                int less = concurrent - runningTasks.size();
                for (int i = 0; i < less; i++) {
                    Task task;

                    try {
                        task = tasksQueue.take();
                    } catch (InterruptedException e) {
                        logger.trace(e);
                        break;
                    }

                    inspect0(task.proxy, task.promise);
                }

                logger.debug("Running tasks: {}/{}", runningTasks.size(), concurrent);

                waitingForTasks();
            }
        }

        private void wakeUp() {
            synchronized (signal) {
                signal.notify();
            }

            logger.trace("try to wake up take thread");
        }

        private void waitingForTasks() {

            logger.trace("Waiting for new tasks to enqueue");
            synchronized (signal) {

                try {
                    signal.wait();
                } catch (InterruptedException e) {
                    logger.trace(e);
                }
            }
        }
    }
}
