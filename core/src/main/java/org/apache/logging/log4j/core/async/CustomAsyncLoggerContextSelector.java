package org.apache.logging.log4j.core.async;

import org.apache.logging.log4j.core.LoggerContext;

import java.net.URI;

public class CustomAsyncLoggerContextSelector extends AsyncLoggerContextSelector {

    @Override
    protected LoggerContext createContext(String name, URI configLocation) {
        return new CustomAsyncLoggerContext(name, null, configLocation);
    }


}
