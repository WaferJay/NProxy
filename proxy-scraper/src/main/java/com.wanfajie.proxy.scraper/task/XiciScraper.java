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

public class XiciScraper implements Scraper<HttpProxy> {

    private static final List<URI> URLS;

    static {
        List<URI> urlsList= new ArrayList<>(5);
        Collections.addAll(urlsList,
                URI.create("https://www.xicidaili.com/nn"),
                URI.create("https://www.xicidaili.com/nt"),
                URI.create("https://www.xicidaili.com/wn"),
                URI.create("https://www.xicidaili.com/wt"),
                URI.create("https://www.xicidaili.com/qq"));

        URLS = Collections.unmodifiableList(urlsList);
    }

    @Override
    public List<URI> urls() {
        return URLS;
    }

    @Override
    public Iterator<HttpProxy> scrape(Document doc) {
        Elements elements = doc.select("table#ip_list > tbody > tr");
        elements.remove(0);  // 移除表格头
        return new ElementIterator<>(this, elements);
    }

    @Override
    public HttpProxy parseOne(Element row) {
        Elements columns = row.select("td");
        String ip = columns.get(1).text().trim();

        String portStr = columns.get(2).text().trim();
        int port = Integer.parseInt(portStr);

        String typeName = columns.get(5).text().trim();
        HttpProxy.Type type;
        if ("QQ代理".equals(typeName)) {
            type = HttpProxy.Type.SOCKS;
        } else {
            type = HttpProxy.Type.valueOf(typeName);
        }

        return new HttpProxy(type, ip, port);
    }
}
