package com.wanfajie.proxy.scraper.inspect;

import com.wanfajie.proxy.HttpProxy;

public class EvaluationReport {
    private HttpProxy target;

    private boolean isAnonymous;

    private int totalDuration;
    private int connectDuration;
    private int transportDuration;
    private long timestamp;

    public EvaluationReport(HttpProxy target, boolean isAnonymous, int totalDuration,
                            int connectDuration, int transportDuration, long timestamp) {

        this.target = target;
        this.isAnonymous = isAnonymous;
        this.totalDuration = totalDuration;
        this.connectDuration = connectDuration;
        this.transportDuration = transportDuration;
        this.timestamp = timestamp;
    }

    public HttpProxy target() {
        return target;
    }

    public boolean isAnonymous() {
        return isAnonymous;
    }

    public int totalDuration() {
        return totalDuration;
    }

    public int connectDuration() {
        return connectDuration;
    }

    public int transportDuration() {
        return transportDuration;
    }

    public long timestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "EvaluationReport{" +
                "target=" + target +
                ", isAnonymous=" + isAnonymous +
                ", totalDuration=" + totalDuration +
                ", connectDuration=" + connectDuration +
                ", transportDuration=" + transportDuration +
                ", timestamp=" + timestamp +
                '}';
    }
}
