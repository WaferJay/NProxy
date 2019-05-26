package com.wanfajie.proxy.scraper.task;

import com.wanfajie.proxy.scraper.Scraper;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public interface ScraperFactory<T> {
    List<Scraper<T>> create(URL config, URL... merge) throws IOException;
    boolean isSupport(Class<T> resultType, URL config);
}
