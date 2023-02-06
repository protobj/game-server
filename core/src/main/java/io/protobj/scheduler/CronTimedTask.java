package io.protobj.scheduler;


import org.springframework.scheduling.support.CronExpression;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

public class CronTimedTask<T> extends OneShotTimedTask<T> {

    private final CronExpression cronExpression;

    public CronTimedTask(long expireTimeMillis, Executor executor, Callable<T> callable, CronExpression cronExpression) {
        super(expireTimeMillis, executor, callable);
        this.cronExpression = cronExpression;
    }

    @Override
    public void tryRepeatOnExecute(HashedWheelTimer.ExpireCallback reAdd, ZoneId zoneId) {
        ZonedDateTime next = this.cronExpression.next(ZonedDateTime.ofInstant(Instant.ofEpochMilli(expireTimeMillis), zoneId));
        if (next != null) {
            this.expireTimeMillis = next.toInstant().toEpochMilli();
            reAdd.onTime(this);
        }
    }
}
