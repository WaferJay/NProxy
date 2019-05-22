package com.wanfajie.proxy.scraper.inspect;

import com.wanfajie.proxy.HttpProxy;

import java.util.function.Consumer;
import java.util.function.Function;

public class InspectorBridge implements Consumer<HttpProxy> {

    private final Inspector inspector;
    private final Consumer<HttpProxy> consumer;
    private final Function<EvaluationReport, Boolean> filter;

    public InspectorBridge(Inspector inspector, Consumer<HttpProxy> consumer) {
        this(inspector, consumer, null);
    }

    public InspectorBridge(Inspector inspector, Consumer<HttpProxy> consumer, Function<EvaluationReport, Boolean> filter) {
        this.inspector = inspector;
        this.consumer = consumer;
        this.filter = filter;
    }

    @Override
    public void accept(HttpProxy proxy) {
        inspector.inspect(proxy).addListener(f -> {

            if (f.isSuccess()) {
                EvaluationReport report = (EvaluationReport) f.get();

                if (filter(report)) {
                    consumer.accept(report.target());
                }
            }
        });
    }

    protected boolean filter(EvaluationReport report) {
        return filter == null || filter.apply(report);
    }
}
