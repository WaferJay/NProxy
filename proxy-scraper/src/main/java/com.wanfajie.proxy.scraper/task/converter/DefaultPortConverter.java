package com.wanfajie.proxy.scraper.task.converter;

import org.jsoup.select.Elements;

public class DefaultPortConverter implements PortConverter {

    @Override
    public Integer convert(Elements elements) {
        String value = StringConverter.INSTANCE.convert(elements);
        return Integer.parseInt(value.toUpperCase());
    }
}
