package com.wanfajie.proxy.scraper;

import java.net.URL;

public interface ScraperEngine<R> {

    void start();
    void stop();

    ScraperEngine<R> register(Scraper<R> scraper, int seconds);
    State state();

    ScraperEngine<R> loadScrapers(URL url);

    void doTask(Scraper<R> scraper);

    enum State {
        READY, RUNNING, FINISHED
    }
}
