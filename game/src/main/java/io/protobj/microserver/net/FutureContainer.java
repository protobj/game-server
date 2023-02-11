package io.protobj.microserver.net;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;

import io.protobj.exception.LogicException;
import io.protobj.util.Jackson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class FutureContainer {

    private static final Logger logger = LoggerFactory.getLogger(FutureContainer.class);
    Cache<Integer, Ask> askCache;

    public <T> FutureContainer() {
        askCache = CacheBuilder.newBuilder()
                .removalListener((RemovalListener<Integer, Ask>) notification -> {
                    if (notification.getCause() != RemovalCause.EXPLICIT) {
                        Object ask = notification.getValue().ask;
                        CompletableFuture future = notification.getValue().getFuture();
                        if (future != null && !future.isDone()) {
                            logger.error("ask callback : {}->{} cause:{}", ask.getClass().getSimpleName(), Jackson.INSTANCE.encode(ask), notification.getCause());
                            future.completeExceptionally(new LogicException(1));
                        }
                        notification.getValue().recycle();
                    }
                })
                .expireAfterAccess(10, TimeUnit.MINUTES).build();
    }
}
