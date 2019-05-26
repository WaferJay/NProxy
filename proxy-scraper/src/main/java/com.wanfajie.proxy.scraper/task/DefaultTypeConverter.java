package com.wanfajie.proxy.scraper.task;

import com.wanfajie.proxy.HttpProxy;

public class DefaultTypeConverter implements TypeConverter {
    @Override
    public HttpProxy.Type convert(String value) {
        return HttpProxy.Type.valueOf(value);
    }
}
