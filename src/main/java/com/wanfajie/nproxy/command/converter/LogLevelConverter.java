package com.wanfajie.nproxy.command.converter;

import com.beust.jcommander.IStringConverter;
import org.apache.logging.log4j.Level;

public class LogLevelConverter implements IStringConverter<Level> {

    @Override
    public Level convert(String s) {
        return Level.valueOf(s.toUpperCase());
    }
}
