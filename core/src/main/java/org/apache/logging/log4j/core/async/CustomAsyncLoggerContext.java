package org.apache.logging.log4j.core.async;

import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.message.MessageFactory;

import java.lang.reflect.Field;
import java.net.URI;

public class CustomAsyncLoggerContext extends AsyncLoggerContext {
    private AsyncLoggerDisruptor loggerDisruptor1;

    public CustomAsyncLoggerContext(String name) {
        super(name);
        init();

    }

    private void init() {
        try {
            Field disruptor = getClass().getSuperclass().getDeclaredField("loggerDisruptor");
            disruptor.setAccessible(true);
            loggerDisruptor1 = (AsyncLoggerDisruptor) disruptor.get(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public CustomAsyncLoggerContext(String name, Object externalContext) {
        super(name, externalContext);
        init();
    }

    public CustomAsyncLoggerContext(String name, Object externalContext, URI configLocn) {
        super(name, externalContext, configLocn);
        init();
    }

    public CustomAsyncLoggerContext(String name, Object externalContext, String configLocn) {
        super(name, externalContext, configLocn);
        init();
    }

    @Override
    protected Logger newInstance(LoggerContext ctx, String name, MessageFactory messageFactory) {
        return new CustomAsyncLogger(ctx, name, messageFactory, loggerDisruptor1);
    }
}
