package com.wanfajie.proxy.scraper.task.converter;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class StringConverter implements Converter<String> {

    public static final StringConverter INSTANCE = new StringConverter();

    @Override
    public String convert(Elements elements) {

        if (elements.size() == 1) {
            return elements.get(0).text().trim();
        }

        StringBuilder sb = new StringBuilder(16);
        for (Element each : elements) {
            String part = each.text().trim();
            sb.append(part);
        }

        return sb.toString();
    }
}
