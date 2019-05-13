package com.wanfajie.proxy.scraper;

public interface ScraperEngine<R> {

    void start();
    void stop();

    ScraperEngine<R> register(Scraper<R> scraper, int seconds);
    State state();

    void doTask(Scraper<R> scraper);

    enum State {
        READY, RUNNING, FINISHED
    }
}
