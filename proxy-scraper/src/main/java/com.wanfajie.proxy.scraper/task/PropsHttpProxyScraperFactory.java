package com.wanfajie.proxy.scraper.task;

import com.wanfajie.proxy.HttpProxy;
import com.wanfajie.proxy.scraper.Scraper;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.StringUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

public class PropsHttpProxyScraperFactory extends AbstractHttpProxyScraperFactory {

    public static final PropsHttpProxyScraperFactory INSTANCE = new PropsHttpProxyScraperFactory();

    private static final InternalLogger logger =
            InternalLoggerFactory.getInstance(PropsHttpProxyScraperFactory.class);

    @Override
    public List<Scraper<HttpProxy>> create(URL config, URL... merge) throws IOException {
        Properties props = new Properties();

        try (InputStream is = config.openStream()) {
            props.load(new InputStreamReader(is, CharsetUtil.UTF_8));
        }

        if (merge != null) {

            for (URL url : merge) {

                try (InputStream is = url.openStream()) {
                    props.load(new InputStreamReader(is, CharsetUtil.UTF_8));
                }
            }
        }

        Map<String, ScraperMeta> metas = new HashMap<>();
        for (Object o : props.keySet()) {
            String key = (String) o;
            String value = props.getProperty(key);

            if (StringUtil.isNullOrEmpty(value)) {
                logger.info("ignored empty option: " + key);
                continue;
            }

            try {
                parseProperty(key, metas, value);
            } catch (IllegalArgumentException e) {
                throw ParseConfFileException.forOption(key, value);
            }
        }


        return metas.values().stream()
                .map(this::validateAndCreateScraper)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isSupport(Class<HttpProxy> resultType, URL config) {
        if (resultType == HttpProxy.class) return true;

        String path = config.toString().toLowerCase();
        return path.endsWith(".properties");
    }

    private static void parseProperty(String propKey, Map<String, ScraperMeta> metas, String value) {

        String[] parts = propKey.split("\\.", 3);

        if (!"scraper".equals(parts[0])) return;
        if (parts.length <= 2) return;

        String name = parts[1];
        String key = parts[2];
        ScraperMeta meta = metas.computeIfAbsent(name, ScraperMeta::new);

        switch (key) {
            case "urls":
                String[] urls = value.split("\\s*,\\s*");
                if (urls.length == 0) {
                    throw new IllegalArgumentException();
                }

                meta.urls = new ArrayList<>(urls.length);
                for (String url : urls) {

                    if (url.isEmpty() || url.trim().isEmpty()) {
                        throw new IllegalArgumentException();
                    }

                    URI uri = URI.create(url.trim());
                    meta.urls.add(uri);
                }
                break;
            case "parse.rows":
                meta.rowsSelect = value;
                break;
            case "parse.host":
                meta.hostSelect = value;
                break;
            case "parse.port":
                meta.portSelect = value;
                break;
            case "parse.type":
                meta.typeSelect = value;
                break;
            case "schedule.delay":
                meta.delay = Integer.parseInt(value);
                break;
            case "schedule.initialDelay":
                meta.initalDelay = Integer.parseInt(value);
                break;
            case "charset":
                meta.charset = Charset.forName(value);
                break;
            case "parse.type.converter":
                meta.typeConverter = value;
                break;
            default:
                logger.info("skipped useless key: " + key);
        }
    }
}
