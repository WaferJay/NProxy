package com.wanfajie.nproxy.command;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import java.io.File;
import java.net.URL;

public final class FullParameters {

    private final JCommander commander;

    public final LogParameter logParams = new LogParameter();
    public final ProxyServerParameter servParams = new ProxyServerParameter();
    public final ScraperParameter scraParams = new ScraperParameter();

    @Parameter(names = {"--help", "-h"}, description = "print this help message and exit", order = 100)
    private boolean help;

    {
        commander = JCommander.newBuilder()
                .addObject(this)
                .addObject(logParams)
                .addObject(servParams)
                .addObject(scraParams)
                .build();
    }

    private FullParameters(String progressName, String... args) {
        commander.setProgramName(progressName);
        commander.parse(args);
    }

    public boolean help() {
        return help;
    }

    public void usage() {
        commander.usage();
    }

    public static FullParameters parse(Class<?> mainClass, String... args) {
        String progressName = getProgressName(mainClass);

        return new FullParameters(progressName, args);
    }

    private static String getProgressName(Class<?> mainClass) {
        URL url = mainClass.getProtectionDomain().getCodeSource().getLocation();
        File file = new File(url.getFile());
        if (file.isFile()) {
            return "java -jar " + file.getName();
        } else {
            return "java " + mainClass.getTypeName();
        }
    }
}
