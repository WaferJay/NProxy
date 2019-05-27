package com.wanfajie.proxy.scraper.task.converter;

import org.jsoup.select.Elements;

public interface Converter<R> {
    R convert(Elements elements);
}
