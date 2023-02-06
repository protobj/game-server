package io.protobj.scheduler;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

public class FixedDelayTimedTask<T> extends OneShotTimedTask<T> {

    private final long period;
    private final HashedWheelTimer.ExpireCallback reAdd;

    public FixedDelayTimedTask(long expireTimeMillis, Executor executor, Callable<T> callable, long period, HashedWheelTimer.ExpireCallback reAdd) {
        super(expireTimeMillis, executor, callable);
        this.period = period;
        this.reAdd = reAdd;
    }

    @Override
    public void run() {
        super.run();
        this.expireTimeMillis = System.currentTimeMillis() + period;
        reAdd.onTime(this);
    }
}
