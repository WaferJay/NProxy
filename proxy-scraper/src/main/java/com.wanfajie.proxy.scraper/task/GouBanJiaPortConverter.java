package com.wanfajie.proxy.scraper.task;

import com.wanfajie.proxy.scraper.task.converter.PortConverter;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.regex.Pattern;

class GouBanJiaPortConverter implements PortConverter {

    private static final Pattern P_PORT_CIPHER = Pattern.compile("^[a-z]+$", Pattern.CASE_INSENSITIVE);

    @Override
    public Integer convert(Elements elements) {
        Element portElem = elements.select(".port").get(0);

        for (String cls : portElem.classNames()) {

            if (P_PORT_CIPHER.matcher(cls).matches()) {
                return decryptPort(cls.toUpperCase());
            }
        }

        throw new IllegalStateException(elements.toString());
    }

    private int decryptPort(String className) {
        int len = className.length();

        int port = 0;
        for (int i = 0; i < len; i++) {
            int code = className.charAt(i) - 'A';
            port = port * 10 + code;
        }

        return port / 8;
    }
}
