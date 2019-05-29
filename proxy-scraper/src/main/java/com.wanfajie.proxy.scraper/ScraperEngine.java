package com.wanfajie.proxy.scraper;

import java.net.URL;

public interface ScraperEngine<R> {

    DefaultScraperEngine<R> register(Scraper<R> scraper);

    void start();
    void stop();

    ScraperEngine<R> register(Scraper<R> scraper, int seconds);
    State state();

    ScraperEngine<R> loadScrapers(URL url);

    enum State {
        READY, RUNNING, FINISHED
    }
}
