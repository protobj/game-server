package io.protobj.scheduler1;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

public class FixedDelayTimedTask<T> extends OneShotTimedTask<T> {

    private final long period;
    private final HashedWheelTimer.ExpireCallback reAdd;

    public FixedDelayTimedTask(long period, Executor executor, Callable<T> callable, HashedWheelTimer.ExpireCallback reAdd) {
        super(System.currentTimeMillis() + period, executor, callable);
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
