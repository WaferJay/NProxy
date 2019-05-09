package com.wanfajie.proxy.scraper.inspect;

import com.wanfajie.proxy.HttpProxy;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;

public interface Inspector {
    default Future<EvaluationReport> inspect(HttpProxy proxy) {
        return inspect(proxy, GlobalEventExecutor.INSTANCE.newPromise());
    }

    Future<EvaluationReport> inspect(HttpProxy proxy, Promise<EvaluationReport> promise);

    void close();
}
