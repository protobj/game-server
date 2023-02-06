package io.protobj.scheduler1;


import org.springframework.scheduling.support.CronExpression;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

public class CronTimedTask<T> extends OneShotTimedTask<T> {

    private final CronExpression cronExpression;

    public CronTimedTask(long expireTimeMillis, Executor executor, Callable<T> callable, String cron, ZoneId zoneId) {
        super(expireTimeMillis, executor, callable);
        this.cronExpression = CronExpression.parse(cron);
        ZonedDateTime next = this.cronExpression.next(ZonedDateTime.now(zoneId));
        this.expireTimeMillis = next.toInstant().toEpochMilli();
    }

    @Override
    public void tryRepeat(HashedWheelTimer.ExpireCallback reAdd, ZoneId zoneId) {
        ZonedDateTime next = this.cronExpression.next(ZonedDateTime.now(zoneId));
        this.expireTimeMillis = next.toInstant().toEpochMilli();
    }
}
