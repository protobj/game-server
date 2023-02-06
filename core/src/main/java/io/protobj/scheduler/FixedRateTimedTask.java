package io.protobj.scheduler;

import java.time.ZoneId;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

public class FixedRateTimedTask<T> extends OneShotTimedTask<T> {

    private final long period;


    public FixedRateTimedTask(long expireTimeMillis, Executor executor, Callable<T> callable,long period) {
        super(expireTimeMillis,executor,callable);
        this.period = period;
    }

    @Override
    public void tryRepeatOnExecute(HashedWheelTimer.ExpireCallback reAdd, ZoneId zoneId) {
        this.expireTimeMillis += period;
        reAdd.onTime(this);
    }
}
