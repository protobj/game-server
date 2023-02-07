package io.protobj.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class OneShotTimedTask<T> extends CompletableFuture<T> implements TimedTask<T> {
    private static final Logger logger = LoggerFactory.getLogger(OneShotTimedTask.class);
    private volatile boolean cancelled = false;//是否取消
    protected volatile long expireTimeMillis;//到期时间

    protected final Executor executor;

    private final Callable<T> callable;

    public OneShotTimedTask(long expireTimeMillis, Executor executor, Callable<T> callable) {
        this.expireTimeMillis = expireTimeMillis;
        this.executor = executor;
        this.callable = callable;
    }

    @Override
    public Executor executor() {
        return executor;
    }

    @Override
    public long expireTimeMillis() {
        return expireTimeMillis;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        cancelled = true;
        return true;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void run() {
        try {
            this.complete(callable.call());
        } catch (Throwable e) {
            logger.error("error", e);
            this.completeExceptionally(e);
        }
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(Math.max(0, expireTimeMillis), TimeUnit.MILLISECONDS);
    }
}
