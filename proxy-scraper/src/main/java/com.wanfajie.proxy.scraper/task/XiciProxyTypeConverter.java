package com.wanfajie.proxy.scraper.task;

import com.wanfajie.proxy.HttpProxy;

class XiciProxyTypeConverter implements TypeConverter {

    @Override
    public HttpProxy.Type convert(String value) {
        if (value.equalsIgnoreCase("QQ代理")) {
            value = "SOCKS";
        }

        return HttpProxy.Type.valueOf(value);
    }
}
