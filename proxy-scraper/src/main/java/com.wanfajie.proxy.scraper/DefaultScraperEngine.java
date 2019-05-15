package com.wanfajie.proxy.scraper;

import com.wanfajie.nttpclient.HttpRequestFlow;
import com.wanfajie.nttpclient.NttpClient;
import io.netty.channel.EventLoop;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.jsoup.nodes.Document;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class DefaultScraperEngine<T> implements ScraperEngine<T>, Closeable, AutoCloseable {

    private final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultScraperEngine.class);

    private final NioEventLoopGroup workers;
    private final Consumer<T> consumer;

    private volatile State state = State.READY;

    private NttpClient httpclient;

    private List<ScraperWrapper<T>> tasks = new ArrayList<>();

    public DefaultScraperEngine(NioEventLoopGroup workers, Consumer<T> consumer) {
        this.workers = workers;
        this.consumer = consumer;

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
        System.out.println(eventExecutors);
        tasks.add(new ScraperWrapper<>(this, scraper, seconds, eventExecutors));
        return this;
    }

    @Override
    public void doTask(Scraper<T> scraper) {

        for (URI url : scraper.urls()) {

            HttpRequestFlow flow = httpclient.get(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:60.0) Gecko/20100101 Firefox/60.0")
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

    @Override
    public void start() {
        if (state == State.READY) {
            tasks = Collections.unmodifiableList(tasks);
        } else {
            throw new AlreadyStartedException();
        }

        state = State.RUNNING;
        for (ScraperWrapper<T> task : tasks) {
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

                logger.debug("Scraped {}: {}", scraper.name(), value);
            }
        }
    }

    private Future<Iterator<T>> handleResponse(Scraper<T> scraper, URI url, FullHttpResponse response) throws IOException {
        EventLoop eventLoop = getScraperWrapper(scraper);
        Document doc = scraper.handleResponse(response, url.toString());
        return eventLoop.submit(() -> scraper.scrape(doc));
    }

    private EventLoop getScraperWrapper(Scraper<T> scraper) {
        ScraperWrapper<T> wrapper = tasks.stream().filter(e -> e.scraper() == scraper)
                .findFirst()
                .orElse(null);

        if (wrapper == null) {
            String name = "[" + scraper.name() + ": " + scraper + "]";
            throw new IllegalStateException("unregistered scraper: " + name);
        }

        return wrapper.eventLoop();
    }

    @Override
    public void close() {
        if (State.RUNNING == state) {
            stop();
        }
    }

    public State state() {
        return state;
    }
}
