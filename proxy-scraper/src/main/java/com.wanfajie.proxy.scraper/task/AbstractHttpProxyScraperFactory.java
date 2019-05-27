package com.wanfajie.proxy.scraper.task;

import com.wanfajie.proxy.HttpProxy;
import com.wanfajie.proxy.scraper.task.converter.*;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.StringUtil;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;

abstract class AbstractHttpProxyScraperFactory implements ScraperFactory<HttpProxy> {

    @SuppressWarnings("unchecked")
    private <C extends Converter<?>> C loadConverter(String classStr, String scraper) {

        Class<?> converterClass;
        try {
            converterClass = Class.forName(classStr);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(scraper + ".parse.type.converter", e);
        }

        try {
            return (C) converterClass.newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassCastException e) {
            throw new IllegalArgumentException(scraper + ".parse.type.converter", e);
        }
    }

    protected HttpProxyScraperImpl validateAndCreateScraper(ScraperMeta meta) {
        String name = meta.name;
        if (StringUtil.isNullOrEmpty(name)) {
            throw new IllegalArgumentException("scraper name must not be empty");
        }

        ObjectUtil.checkNotNull(meta.urls, name + ".urls");
        ObjectUtil.checkNonEmpty(meta.urls, name + ".urls");
        ObjectUtil.checkNotNull(meta.rowsSelect, name + ".parse.rows");
        ObjectUtil.checkNotNull(meta.hostSelect, name + ".parse.host");
        ObjectUtil.checkNotNull(meta.portSelect, name + ".parse.port");
        ObjectUtil.checkNotNull(meta.typeSelect, name + ".parse.type");

        ObjectUtil.checkPositive(meta.delay, name + ".schedule.delay");
        ObjectUtil.checkPositiveOrZero(meta.initalDelay, name + ".schedule.initialDelay");

        HostConverter hostConverter;
        if (meta.hostConverter != null) {
            hostConverter = loadConverter(meta.hostConverter, name);
        } else {
            hostConverter = new DefaultHostConverter();
        }

        PortConverter portConverter;
        if (meta.portConverter != null) {
            portConverter = loadConverter(meta.portConverter, name);
        } else {
            portConverter = new DefaultPortConverter();
        }

        TypeConverter typeConverter;
        if (meta.typeConverter != null) {
            typeConverter = loadConverter(meta.typeConverter, name);
        } else {
            typeConverter = new DefaultTypeConverter();
        }

        return new HttpProxyScraperImpl(meta.name, meta.urls, meta.rowsSelect,
                meta.hostSelect, meta.portSelect, meta.typeSelect, meta.delay,
                meta.initalDelay, meta.charset, hostConverter, portConverter,
                typeConverter);
    }

    static final class ScraperMeta {
        Charset charset = CharsetUtil.UTF_8;

        String name;
        List<URI> urls;
        int initalDelay;
        int delay;

        String rowsSelect;
        String hostSelect;
        String portSelect;
        String typeSelect;

        String hostConverter;
        String portConverter;
        String typeConverter;

        ScraperMeta(String name) {
            this.name = name;
        }
    }
}
