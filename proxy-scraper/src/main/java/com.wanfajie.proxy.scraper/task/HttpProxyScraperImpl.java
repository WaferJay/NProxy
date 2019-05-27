package com.wanfajie.proxy.scraper.task;

import com.wanfajie.proxy.HttpProxy;
import com.wanfajie.proxy.scraper.ElementIterator;
import com.wanfajie.proxy.scraper.Scraper;
import com.wanfajie.proxy.scraper.task.converter.HostConverter;
import com.wanfajie.proxy.scraper.task.converter.PortConverter;
import com.wanfajie.proxy.scraper.task.converter.TypeConverter;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class HttpProxyScraperImpl implements Scraper<HttpProxy> {

    private final String name;
    private final List<URI> urls;
    private final String rowsSelect;

    private final String hostSelect;
    private final String portSelect;
    private final String typeSelect;

    private final int delay;
    private final int initialDelay;

    private final Charset charset;

    private final HostConverter hostConverter;
    private final PortConverter portConverter;
    private final TypeConverter typeConverter;

    HttpProxyScraperImpl(String name, List<URI> urls, String rowsSelect,
                         String hostSelect, String portSelect, String typeSelect,
                         int delay, int initialDelay, Charset charset,
                         HostConverter hostConvert, PortConverter portConverter,
                         TypeConverter typeConverter) {

        this.name = name;
        this.urls = urls;

        this.rowsSelect = rowsSelect;
        this.hostSelect = hostSelect;
        this.portSelect = portSelect;
        this.typeSelect = typeSelect;

        this.delay = delay;
        this.initialDelay = initialDelay;

        this.charset = charset;

        this.hostConverter = hostConvert;
        this.portConverter = portConverter;
        this.typeConverter = typeConverter;
    }

    @Override
    public List<URI> urls() {
        return urls;
    }

    @Override
    public Iterator<HttpProxy> scrape(Document doc) {
        Elements elements = doc.select(rowsSelect);
        return new ElementIterator<>(this, elements);
    }

    @Override
    public HttpProxy parseOne(Element row) {
        Elements hostElems = checkNotEmpty(row.select(hostSelect));
        Elements portElems = checkNotEmpty(row.select(portSelect));
        Elements typeElems = checkNotEmpty(row.select(typeSelect));

        try {
            String host = hostConverter.convert(hostElems);
            int port = portConverter.convert(portElems);
            HttpProxy.Type type = typeConverter.convert(typeElems);

            return new HttpProxy(type, host, port);
        } catch (Exception e) {
            throw new IllegalStateException("Conversion failed [Scraper: " + name + "]", e);
        }
    }

    private <T extends Collection> T checkNotEmpty(T collection) {
        if (collection.isEmpty()) {
            throw new IllegalStateException("wrong host selector in " + name + "scraper");
        }

        return collection;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public int delay() {
        return delay;
    }

    @Override
    public int initialDelay() {
        return initialDelay;
    }

    public String getHostSelect() {
        return hostSelect;
    }

    public String getPortSelect() {
        return portSelect;
    }

    public String getRowsSelect() {
        return rowsSelect;
    }

    public String getTypeSelect() {
        return typeSelect;
    }

    @Override
    public Charset charset() {
        return charset;
    }
}
