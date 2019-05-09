package com.wanfajie.proxy.scraper.task;

import com.wanfajie.proxy.HttpProxy;
import com.wanfajie.proxy.scraper.ElementIterator;
import com.wanfajie.proxy.scraper.Scraper;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class Data5UScraper implements Scraper<HttpProxy> {

    private static final List<URI> URLS;
    static {
        List<URI> urlsList= new ArrayList<>(3);
        Collections.addAll(urlsList,
                URI.create("http://www.data5u.com/free/index.html"),
                URI.create("http://www.data5u.com/free/gngn/index.shtml"),
                URI.create("http://www.data5u.com/free/gnpt/index.shtml"));

        URLS = Collections.unmodifiableList(urlsList);
    }

    @Override
    public List<URI> urls() {
        return URLS;
    }

    @Override
    public Iterator<HttpProxy> scrape(Document doc) {
        Elements rows = doc.select("div[class='wlist'] > ul > li > ul[class='l2']");
        return new ElementIterator<>(this, rows);
    }

    @Override
    public HttpProxy parseOne(Element row) {
        Elements columns = row.select("span > li");
        String ipStr = columns.eq(0).text().trim();
        String portStr = columns.eq(1).text().trim();
        int port = Integer.parseInt(portStr);
        String typeName = columns.eq(3).text().trim().toUpperCase();
        HttpProxy.Type type = HttpProxy.Type.valueOf(typeName);

        return new HttpProxy(type, ipStr, port);
    }
}
