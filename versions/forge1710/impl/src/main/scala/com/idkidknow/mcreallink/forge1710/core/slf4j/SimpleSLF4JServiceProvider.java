package com.idkidknow.mcreallink.forge1710.core.slf4j;

import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;

public class SimpleSLF4JServiceProvider implements SLF4JServiceProvider {
    @Override
    public ILoggerFactory getLoggerFactory() {
        return Log4jLoggerFactory$.MODULE$;
    }

    @Override
    public IMarkerFactory getMarkerFactory() {
        return Log4jMarkerFactory$.MODULE$;
    }

    @Override
    public MDCAdapter getMDCAdapter() {
        return Log4jMDCAdapter$.MODULE$;
    }

    @Override
    public String getRequestedApiVersion() {
        return "2.0.99";
    }

    @Override
    public void initialize() {}
}
