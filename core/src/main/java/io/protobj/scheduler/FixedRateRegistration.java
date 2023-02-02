package io.protobj.scheduler;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

class FixedRateRegistration<T> extends OneShotRegistration<T> {

  private final int rescheduleRounds;
  private final int scheduleOffset;

  public FixedRateRegistration(int rounds,
                               Callable<T> callable,
                               long delay,
                               Executor executor,
                               int rescheduleRounds,
                               int scheduleOffset) {
    super(rounds, callable, delay,executor);
    this.rescheduleRounds = rescheduleRounds;
    this.scheduleOffset = scheduleOffset;
  }

  public int getOffset() {
    return this.scheduleOffset;
  }

  public void reset() {
    this.status = Status.READY;
    this.rounds = rescheduleRounds;

  }

  @Override
  public boolean isCancelAfterUse() {
    return false;
  }

}
