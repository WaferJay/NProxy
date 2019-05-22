package com.wanfajie.nproxy.command;

import com.beust.jcommander.Parameter;

public class ScraperParameter {

    @Parameter(names = {"--scraper-periodic"})
    private int periodic = 300;

    @Parameter(names = {"--scraper-connect-timeout"})
    private int connectTimeout;

    @Parameter(names = {"--scraper-timeout"})
    private int readTimeout;

    public int periodic() {
        return periodic;
    }

    public int connectTimeout() {
        return connectTimeout;
    }

    public int readTimeout() {
        return readTimeout;
    }
}
