package org.apache.logging.log4j.core.async;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;

public class CustomAsyncLogger extends AsyncLogger {
    /**
     * Constructs an {@code AsyncLogger} with the specified context, name and message factory.
     *
     * @param context         context of this logger
     * @param name            name of this logger
     * @param messageFactory  message factory of this logger
     * @param loggerDisruptor helper class that logging can be delegated to. This object owns the Disruptor.
     */
    public CustomAsyncLogger(LoggerContext context, String name, MessageFactory messageFactory, AsyncLoggerDisruptor loggerDisruptor) {
        super(context, name, messageFactory, loggerDisruptor);
    }

    private static final ThreadLocal<MarkerManager.Log4jMarker> markerThreadLocal = ThreadLocal.withInitial(() -> {
        Thread thread = Thread.currentThread();
        ThreadGroup threadGroup = thread.getThreadGroup();
        String name = threadGroup.getName();
        if (name.contains(":")) {
            name = name.replace(":", "#");
        }
        return new MarkerManager.Log4jMarker(name);
    });

    private MarkerManager.Log4jMarker getMarker() {
        return markerThreadLocal.get();
    }

    @Override
    public void logMessage(final String fqcn, final Level level, Marker marker, final Message message,
                           final Throwable thrown) {
        if (marker == null) {
            marker = getMarker();
        }
        super.logMessage(fqcn, level, marker, message, thrown);
    }

    @Override
    public void log(final Level level, Marker marker, final String fqcn, final StackTraceElement location,
                    final Message message, final Throwable throwable) {
        if (marker == null) {
            marker = getMarker();
        }
        super.log(level, marker, fqcn, location, message, throwable);
    }
}
