package com.wanfajie.proxy.scraper.task;

import com.wanfajie.proxy.HttpProxy;
import com.wanfajie.proxy.scraper.task.converter.StringConverter;
import com.wanfajie.proxy.scraper.task.converter.TypeConverter;
import org.jsoup.select.Elements;

class XiciProxyTypeConverter implements TypeConverter {

    @Override
    public HttpProxy.Type convert(Elements elements) {

        String value = StringConverter.INSTANCE.convert(elements);

        if (value.equalsIgnoreCase("QQ代理")) {
            value = "SOCKS";
        }

        return HttpProxy.Type.valueOf(value.toUpperCase());
    }
}
