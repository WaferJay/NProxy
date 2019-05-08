package com.wanfajie.nttpclient.strategy;

import com.wanfajie.nttpclient.config.NttpClientConfig;

public interface HttpClientStrategyFactory {
    HttpClientStrategy create(NttpClientConfig config, boolean httpsMode);
}
