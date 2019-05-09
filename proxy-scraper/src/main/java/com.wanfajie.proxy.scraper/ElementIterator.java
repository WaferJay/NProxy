package com.wanfajie.proxy.scraper;

import org.jsoup.select.Elements;

import java.util.Iterator;

public class ElementIterator<R> implements Iterator<R> {

    private final Scraper<R> scraper;
    private final Elements rows;
    private int index = 0;

    public ElementIterator(Scraper<R> scraper, Elements rows) {
        this.scraper = scraper;
        this.rows = rows;
    }

    @Override
    public boolean hasNext() {
        return index < rows.size();
    }

    @Override
    public R next() {
        R product = null;
        try {
            product = scraper.parseOne(rows.get(index++));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return product;
    }
}
