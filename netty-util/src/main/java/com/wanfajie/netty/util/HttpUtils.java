package com.wanfajie.netty.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;

public class HttpUtils {

    private HttpUtils() {
        throw new UnsupportedOperationException();
    }

    public static String urlencode(String str) {
        try {
            return URLEncoder.encode(str, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String urldecode(String str) {
        try {
            return URLDecoder.decode(str, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    public static StringBuilder urlencode(StringBuilder sb, Map<String, String> paramsMap) {
        for (Map.Entry<String, String> entry: paramsMap.entrySet()) {
            String value = urlencode(entry.getValue());
            sb.append(entry.getKey())
              .append("=")
              .append(value)
              .append("&");
        }

        if (!paramsMap.isEmpty()) {
            sb.deleteCharAt(sb.length()-1);
        }

        return sb;
    }

    public static String urlencode(Map<String, String> paramsMap) {
        return urlencode(new StringBuilder(256), paramsMap).toString();
    }
}
