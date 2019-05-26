package com.wanfajie.proxy.scraper.task;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public final class ScraperFactoryManager {

    private static final List<ScraperFactory> SCRAPER_FACTORIES = new ArrayList<>();

    static {
        register(PropsHttpProxyScraperFactory.INSTANCE);
    }

    private ScraperFactoryManager() {
        throw new UnsupportedOperationException();
    }

    static void register(ScraperFactory factory) {
        SCRAPER_FACTORIES.add(factory);
    }

    @SuppressWarnings("unchecked")
    public static <T> ScraperFactory<T> getFactory(Class<T> resultType, URL url) {
        return SCRAPER_FACTORIES.stream()
                .filter(e -> e.isSupport(resultType, url))
                .findFirst()
                .orElse(null);
    }
}
