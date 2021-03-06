package com.wanfajie.proxy.scraper;

import com.wanfajie.nttpclient.HttpRequestFlow;
import com.wanfajie.nttpclient.NttpClient;
import com.wanfajie.proxy.scraper.task.ScraperFactory;
import com.wanfajie.proxy.scraper.task.ScraperFactoryManager;
import io.netty.channel.EventLoop;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.ScheduledFuture;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public abstract class DefaultScraperEngine<T> implements ScraperEngine<T> {

    private final InternalLogger logger = InternalLoggerFactory.getInstance(this.getClass());

    private final NioEventLoopGroup workers;
    private final Consumer<T> consumer;

    private volatile State state = State.READY;

    private NttpClient httpclient;

    private List<ScraperWrapper> tasks = new ArrayList<>();
    private List<URL> configList = new LinkedList<>();

    private Class<?> productClass;

    public DefaultScraperEngine(NioEventLoopGroup workers, Consumer<T> consumer) {
        this.workers = workers;
        this.consumer = consumer;

        ParameterizedType type = (ParameterizedType) getClass().getGenericSuperclass();
        productClass = (Class<?>) type.getActualTypeArguments()[0];

        buildHttpClient();
    }

    private void buildHttpClient() {
        httpclient = new NttpClient.Builder()
                .group(workers)
                .channel(NioSocketChannel.class)
                .maxConnectionPerServer(1)
                .build();
    }

    @Override
    public DefaultScraperEngine<T> register(Scraper<T> scraper, int seconds) {
        EventLoop eventExecutors = workers.next();
        tasks.add(new ScraperWrapper(scraper, eventExecutors, seconds));
        return this;
    }

    @Override
    public DefaultScraperEngine<T> register(Scraper<T> scraper) {
        return register(scraper, 0);
    }

    private void doTask(Scraper<T> scraper) {

        for (URI url : scraper.urls()) {

            HttpRequestFlow flow = httpclient.get(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:60.0) Gecko/20100101 Firefox/60.0");

            handleRequest(flow)
                    .onError(e -> handleException(scraper, e))
                    .onResponse(resp -> {
                        handleResponse(scraper, url, resp)
                            .addListener(f -> handleExceptionFuture(scraper, f))
                            .addListener(f -> handleResultFuture(scraper, f));
                    });

            try {
                scraper.onDownload(flow);
                flow.request();
            } catch (Exception e) {
                handleException(scraper, e);
            }
        }
    }

    protected HttpRequestFlow handleRequest(HttpRequestFlow flow) {
        return flow;
    }

    @Override
    public void start() {
        if (state != State.READY) {
            throw new AlreadyStartedException();
        }

        if (!configList.isEmpty()) {
            URL first = configList.get(0);
            ScraperFactory factory = ScraperFactoryManager.getFactory(productClass, first);
            List<Scraper<T>> scrapers;
            try {
                List<URL> factories = configList.subList(1, configList.size());
                scrapers = factory.create(first, factories.toArray(new URL[0]));
            } catch (IOException e) {
                throw new IllegalArgumentException(configList.toString());
            }

            for (Scraper<T> scraper : scrapers) {
                register(scraper, 600);
            }
        }

        tasks = Collections.unmodifiableList(tasks);
        state = State.RUNNING;

        for (ScraperWrapper task : tasks) {
            task.schedule();
        }
    }

    @Override
    public void stop() {
        if (State.RUNNING != state) {
            return;
        }

        state = State.FINISHED;
        tasks.forEach(t -> t.schedule().cancel(false));
        tasks.forEach(t -> t.schedule().awaitUninterruptibly());

        try {
            httpclient.close();
        } catch (IOException ignored) {
        }
    }

    private void handleExceptionFuture(Scraper<T> scraper, Future<?> future) {
        if (!future.isSuccess()) {
            handleException(scraper, future.cause());
        }
    }

    protected void handleException(Scraper<T> scraper, Throwable throwable) {
        logger.warn("Scrape fail [task: {}]", scraper.name(), throwable);
    }

    @SuppressWarnings("unchecked")
    private void handleResultFuture(Scraper<T> scraper, Future<? super Iterator<T>> future) throws ExecutionException, InterruptedException {

        if (future.isSuccess()) {
            for (Iterator<T> it = (Iterator<T>) future.get(); it.hasNext(); ) {
                T value = it.next();
                if (value != null) {
                    DefaultScraperEngine.this.consumer.accept(value);
                }

                logger.debug("Scraped from {}: {}", scraper.name(), value);
            }
        }
    }

    private Future<Iterator<T>> handleResponse(Scraper<T> scraper, URI url, FullHttpResponse response) throws IOException {
        EventLoop eventLoop = getScraperWrapper(scraper);
        Document doc = scraper.handleResponse(response, url.toString());
        return eventLoop.submit(() -> scraper.scrape(doc));
    }

    private EventLoop getScraperWrapper(Scraper<T> scraper) {
        ScraperWrapper wrapper = tasks.stream().filter(e -> e.scraper() == scraper)
                .findFirst()
                .orElse(null);

        if (wrapper == null) {
            String name = "[" + scraper.name() + ": " + scraper + "]";
            throw new IllegalStateException("unregistered scraper: " + name);
        }

        return wrapper.eventLoop();
    }

    public State state() {
        return state;
    }

    @Override
    public DefaultScraperEngine<T> loadScrapers(URL url) {
        if (state == State.READY) {
            configList.add(url);
        } else {
            throw new IllegalStateException(state.toString());
        }

        return this;
    }

    private final class ScraperWrapper {

        private final EventLoop eventLoop;
        private final Scraper<T> wrapped;
        private final int delay;
        private ScheduledFuture<?> scheduledFuture;

        ScraperWrapper(Scraper<T> scraper, EventLoop eventLoop, int delay) {
            this.wrapped = scraper;
            this.eventLoop = eventLoop;
            this.delay = delay;
        }

        EventLoop eventLoop() {
            return eventLoop;
        }

        ScheduledFuture<?> schedule() {

            if (scheduledFuture == null) {

                int delay = this.delay;
                if (delay == 0) {
                    delay = wrapped.delay();
                }

                scheduledFuture = eventLoop.scheduleWithFixedDelay(() -> {

                    if (state() == ScraperEngine.State.RUNNING) {
                        doTask(wrapped);
                    }

                }, wrapped.initialDelay(), delay, TimeUnit.SECONDS);
            }

            return scheduledFuture;
        }

        Scraper<T> scraper() {
            return wrapped;
        }
    }
}
