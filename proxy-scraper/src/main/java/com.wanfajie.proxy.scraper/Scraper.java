package com.wanfajie.proxy.scraper;

import com.wanfajie.nttpclient.HttpRequestParamFlow;
import io.netty.buffer.ByteBufInputStream;
import io.netty.handler.codec.http.FullHttpResponse;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Iterator;
import java.util.List;

public interface Scraper<R> {
    List<URI> urls();
    Iterator<R> scrape(Document doc);
    R parseOne(Element row);

    default String name() {
        return getClass().getSimpleName();
    }

    default void onDownload(HttpRequestParamFlow request) {}

    default Document handleResponse(FullHttpResponse response, String url) throws IOException {
        if (response.status().code() != 200) {
            String phrase = response.status().reasonPhrase();
            int code = response.status().code();
            throw new HttpStatusException(phrase, code, url);
        }
        InputStream is = new ByteBufInputStream(response.content());
        return Jsoup.parse(is, "UTF-8", "");
    }
}
