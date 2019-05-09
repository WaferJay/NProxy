package com.wanfajie.proxy.scraper.inspect.httpbin;

import com.wanfajie.proxy.scraper.inspect.InspectorChannelHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.util.CharsetUtil;

public class HttpbinInspectorChannelHandler extends InspectorChannelHandler {

    /**
     *
     * @param jsonStr JSON fetch from http://httpbin.org/ip
     *
     * {
     *   "origin": "0.0.0.0, 0.0.0.0"
     * }
     *
     * @return value of origin
     */
    private static String parseJsonOriginField(String jsonStr) {
        int keyIndex = jsonStr.indexOf(":");
        if (keyIndex < 0) {
            throw new IndexOutOfBoundsException();
        }

        int leftQuote = jsonStr.indexOf("\"", keyIndex+1);
        int rightQuote = jsonStr.indexOf("\"", leftQuote+1);

        return jsonStr.substring(leftQuote+1, rightQuote);
    }

    private static <T> boolean equalsAll(T[] array, T value) {
        boolean equ = true;

        if (array.length > 0) {

            for (int i = 0; equ && i < array.length; i++) {
                equ = array[i].equals(value);
            }
        }

        return equ;
    }

    protected boolean isAnonymous(FullHttpResponse response) {
        String jsonStr = response.content().toString(CharsetUtil.US_ASCII);
        String originField = parseJsonOriginField(jsonStr);
        String[] ips = originField.split("\\s+,\\s+");

        return equalsAll(ips, proxy().getHost());
    }
}
