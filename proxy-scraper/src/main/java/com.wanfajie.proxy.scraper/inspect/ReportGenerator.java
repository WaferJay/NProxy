package com.wanfajie.proxy.scraper.inspect;

import com.wanfajie.proxy.HttpProxy;

public interface ReportGenerator {
    EvaluationReport createReport();
    HttpProxy proxy();
}
