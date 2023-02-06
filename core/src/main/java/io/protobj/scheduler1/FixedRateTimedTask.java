package io.protobj.scheduler1;

import java.time.ZoneId;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

public class FixedRateTimedTask<T> extends OneShotTimedTask<T> {

    private final long period;


    public FixedRateTimedTask(long period, Executor executor, Callable<T> callable) {
        super(period + System.currentTimeMillis(), executor, callable);
        this.period = period;
    }

    @Override
    public void tryRepeat(HashedWheelTimer.ExpireCallback reAdd, ZoneId zoneId) {
        this.expireTimeMillis += period;
        reAdd.onTime(this);
    }
}
