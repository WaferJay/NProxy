package com.wanfajie.nproxy.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(separators = "=")
public class InspectorParameter {

    @Parameter(names = {"--inspector-max-concurrent", "-ic"}, order = 40)
    private int concurrent = 100;

    @Parameter(names = {"--inspector-connect-timeout"}, order = 40)
    private int connectTimeout = 180;

    public int concurrent() {
        return concurrent;
    }

    public int connectTimeout() {
        return connectTimeout;
    }
}
