package io.protobj.scheduler;

import org.apache.logging.log4j.core.util.CronExpression;

import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public class CronRegistration<T> extends OneShotRegistration<T> {
    private CronExpression expression;
    private int nextRounds;

    private final Consumer<CronRegistration<?>> rescheduleCallback;

    public CronRegistration(int rounds, Callable<T> callable, long delay, Executor executor, Consumer<CronRegistration<?>> rescheduleCallback) {
        super(rounds, callable, delay, executor);
        this.rescheduleCallback = rescheduleCallback;
    }

    @Override
    public void reset() {
        this.status = Status.READY;
        this.rounds = nextRounds;
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Override
    public boolean ready() {
        boolean ready = super.ready();
        if (ready) {
            rescheduleCallback.accept(this);
        }
        return ready;
    }

    @Override
    public boolean isCancelAfterUse() {
        return nextRounds == 0;
    }

    public void setNextRounds(int nextRounds) {
        this.nextRounds = nextRounds;
    }
}
