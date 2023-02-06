package io.protobj.scheduler;

import java.time.ZoneId;
import java.util.concurrent.*;

public interface TimedTask<T> extends ScheduledFuture<T>, Runnable {

    @Override
    T get() throws InterruptedException, ExecutionException;

    @Override
    T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException;

    Executor executor();

    boolean isCancelled();

    long expireTimeMillis();

    @Override
    default int compareTo(Delayed o) {
        TimedTask<?> other = (TimedTask<?>) o;
        long r1 = expireTimeMillis();
        long r2 = other.expireTimeMillis();
        if (r1 == r2) {
            return other == this ? 0 : -1;
        } else {
            return Long.compare(r1, r2);
        }
    }

    default void tryRepeatOnExecute(HashedWheelTimer.ExpireCallback reAdd, ZoneId zoneId) {

    }
}
