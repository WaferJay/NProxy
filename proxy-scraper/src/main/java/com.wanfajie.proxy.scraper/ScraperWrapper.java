package com.wanfajie.proxy.scraper;

import io.netty.channel.EventLoop;
import io.netty.util.concurrent.ScheduledFuture;

import java.util.concurrent.TimeUnit;

final class ScraperWrapper<R> {

    private final EventLoop eventLoop;
    private final Scraper<R> wrapped;
    private ScheduledFuture<?> scheduledFuture;
    private ScraperEngine<R> engine;

    ScraperWrapper(DefaultScraperEngine<R> engine, Scraper<R> scraper, EventLoop eventLoop) {
        this.engine = engine;
        this.wrapped = scraper;
        this.eventLoop = eventLoop;
    }

    public EventLoop eventLoop() {
        return eventLoop;
    }

    public ScheduledFuture<?> schedule() {

        if (scheduledFuture == null) {
            scheduledFuture = eventLoop.scheduleWithFixedDelay(() -> {

                if (engine.state() == ScraperEngine.State.RUNNING) {
                    engine.doTask(wrapped);
                }
            }, 0, wrapped.schedule(), TimeUnit.SECONDS);
        }

        return scheduledFuture;
    }

    public Scraper<R> scraper() {
        return wrapped;
    }
}
