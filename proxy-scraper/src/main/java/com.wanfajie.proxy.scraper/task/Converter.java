package com.wanfajie.proxy.scraper.task;

interface Converter<R> {
    R convert(String value);
}
