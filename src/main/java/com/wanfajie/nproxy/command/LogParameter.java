package com.wanfajie.nproxy.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.FileConverter;
import com.wanfajie.nproxy.command.converter.LogLevelConverter;
import org.apache.logging.log4j.Level;

import java.io.File;

@Parameters(separators = "=")
public final class LogParameter {

    @Parameter(names = "--loglevel", converter = LogLevelConverter.class, description = "logging level")
    private Level logLevel = null;

    @Parameter(names = "--logconf", converter = FileConverter.class)
    private File logConfigFile = null;

    public Level getLogLevel() {
        return logLevel;
    }

    public File getLogConfigFile() {
        return logConfigFile;
    }
}
