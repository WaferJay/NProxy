package com.wanfajie.nproxy.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.FileConverter;

import java.io.File;

@Parameters(separators = "=")
public class ScraperParameter {

    @Parameter(names = {"--scraper-config"}, converter = FileConverter.class, order = 30)
    private File scrapersConfig;

    @Parameter(names = {"--disable-default-scrapers"}, order = 31, description = "disable the default scrapers configuration")
    private boolean disableDefault = false;

    public File scrapersConfig() {
        return scrapersConfig;
    }

    public boolean isDisableDefault() {
        return disableDefault;
    }
}
