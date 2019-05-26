package com.wanfajie.proxy.scraper.task;

import com.wanfajie.proxy.HttpProxy;
import com.wanfajie.proxy.scraper.ElementIterator;
import com.wanfajie.proxy.scraper.Scraper;
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

    private int delay;
    private int initialDelay;

    private Charset charset;
    private TypeConverter typeConverter;

    HttpProxyScraperImpl(String name, List<URI> urls, String rowsSelect,
                         String hostSelect, String portSelect, String typeSelect,
                         int delay, int initialDelay, Charset charset,
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

        String host = parseText(hostElems);
        String portStr = parseText(portElems);
        int port = Integer.parseInt(portStr);

        String typeStr = parseText(typeElems).toUpperCase();
        HttpProxy.Type type = typeConverter.convert(typeStr);

        return new HttpProxy(type, host, port);
    }

    private <T extends Collection> T checkNotEmpty(T collection) {
        if (collection.isEmpty()) {
            throw new IllegalStateException("wrong host selector in " + name + "scraper");
        }

        return collection;
    }

    private static String parseText(Elements elements) {
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
