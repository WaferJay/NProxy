package com.wanfajie.proxy.scraper.task;

import com.wanfajie.proxy.scraper.task.converter.HostConverter;
import com.wanfajie.proxy.scraper.task.converter.StringConverter;
import org.jsoup.select.Elements;

public class ShenjiHostConverter implements HostConverter {

    @Override
    public String convert(Elements elements) {
        String address = StringConverter.INSTANCE.convert(elements);

        int portIndex = address.indexOf(":");
        if (portIndex < 0) {
            throw new IndexOutOfBoundsException("':' not found in \"" + address + "\"");
        }

        return address.substring(0, portIndex);
    }
}
