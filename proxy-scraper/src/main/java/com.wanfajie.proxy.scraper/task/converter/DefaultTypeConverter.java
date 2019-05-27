package com.wanfajie.proxy.scraper.task.converter;

import com.wanfajie.proxy.HttpProxy;
import org.jsoup.select.Elements;

public class DefaultTypeConverter implements TypeConverter {

    @Override
    public HttpProxy.Type convert(Elements elements) {
        String value = StringConverter.INSTANCE.convert(elements);
        return HttpProxy.Type.valueOf(value.toUpperCase());
    }
}
