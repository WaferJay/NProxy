package com.wanfajie.proxy.scraper.task;

public class ParseConfFileException extends IllegalArgumentException {

    ParseConfFileException(String msg, Throwable throwable) {
        super(msg, throwable);
    }

    ParseConfFileException(String msg) {
        super(msg);
    }

    static ParseConfFileException forOption(String key, String value) {
        return new ParseConfFileException(key + " = \"" + value + '"');
    }

    static ParseConfFileException forOption(String key, String value, Throwable throwable) {
        return new ParseConfFileException(key + " = \"" + value + '"', throwable);
    }
}

