package com.wanfajie.proxy.scraper;

import com.wanfajie.proxy.HttpProxy;
import io.netty.channel.nio.NioEventLoopGroup;

import java.util.function.Consumer;

public final class HttpProxyScraperEngine extends DefaultScraperEngine<HttpProxy> {

    public HttpProxyScraperEngine(NioEventLoopGroup workers, Consumer<HttpProxy> consumer) {
        super(workers, consumer);
    }
}
