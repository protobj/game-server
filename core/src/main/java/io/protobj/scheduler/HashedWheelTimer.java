package io.protobj.scheduler;

import io.protobj.thread.CustomThreadFactory;

import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Hash Wheel Timer, as per the paper:
 * <p>
 * Hashed and hierarchical timing wheels:
 * http://www.cs.columbia.edu/~nahum/w6998/papers/ton97-timing-wheels.pdf
 * <p>
 * More comprehensive slides, explaining the paper can be found here:
 * http://www.cse.wustl.edu/~cdgill/courses/cs6874/TimingWheels.ppt
 * <p>
 * Hash Wheel timer is an approximated timer that allows performant execution of
 * larger amount of tasks with better performance compared to traditional scheduling.
 *
 * @author Oleksandr Petrov
 */
public class HashedWheelTimer {

    //检查超时间隔
    public static final long DEFAULT_RESOLUTION = TimeUnit.NANOSECONDS.convert(33, TimeUnit.MILLISECONDS);
    //时间轮数
    public static final int DEFAULT_WHEEL_SIZE = 512;
    private final Set<Registration<?>>[] wheel;
    private final int wheelSize;
    private final long resolution;
    private final ExecutorService loop;
    private final WaitStrategy waitStrategy;

    private volatile int cursor = 0;

    public HashedWheelTimer(ThreadGroup group, long res, int wheelSize, WaitStrategy strategy) {
        this.waitStrategy = strategy;

        this.wheel = new Set[wheelSize];
        for (int i = 0; i < wheelSize; i++) {
            wheel[i] = new ConcurrentSkipListSet<>();
        }

        this.wheelSize = wheelSize;

        this.resolution = res;
        final Runnable loopRunnable = () -> {
            long deadline = System.nanoTime();

            while (true) {
                Set<Registration<?>> registrations = wheel[cursor];

                for (Registration<?> r : registrations) {
                    if (r.isCancelled()) {
                        registrations.remove(r);
                    } else if (r.ready()) {
                        r.executor().execute(r);
                        registrations.remove(r);

                        if (!r.isCancelAfterUse()) {
                            reschedule(r);
                        }
                    } else {
                        r.decrement();
                    }
                }

                deadline += resolution;

                try {
                    waitStrategy.waitUntil(deadline);
                } catch (InterruptedException e) {
                    return;
                }

                cursor = (cursor + 1) % wheelSize;
            }
        };
        this.loop = Executors.newSingleThreadExecutor(CustomThreadFactory.create(group,"HashedWheelTimer"));
        this.loop.submit(loopRunnable);
    }


    public ScheduledFuture<?> submit(Executor executor, Runnable runnable) {
        return scheduleOneShot(resolution, constantlyNull(runnable), executor);
    }


    public ScheduledFuture<?> schedule(Executor executor, Runnable runnable,
                                       long period,
                                       TimeUnit timeUnit) {
        return scheduleOneShot(TimeUnit.NANOSECONDS.convert(period, timeUnit),
                constantlyNull(runnable), executor);
    }


    public <V> ScheduledFuture<V> schedule(Executor executor, Callable<V> callable, long period, TimeUnit timeUnit) {
        return scheduleOneShot(TimeUnit.NANOSECONDS.convert(period, timeUnit),
                callable, executor);
    }


    public ScheduledFuture<?> scheduleAtFixedRate(Executor executor, Runnable runnable, long initialDelay, long period, TimeUnit unit) {
        return scheduleFixedRate(TimeUnit.NANOSECONDS.convert(period, unit),
                TimeUnit.NANOSECONDS.convert(initialDelay, unit),
                constantlyNull(runnable),executor);
    }


    public ScheduledFuture<?> scheduleWithFixedDelay(Executor executor, Runnable runnable, long initialDelay, long delay, TimeUnit unit) {
        return scheduleFixedDelay(TimeUnit.NANOSECONDS.convert(delay, unit),
                TimeUnit.NANOSECONDS.convert(initialDelay, unit),
                constantlyNull(runnable),executor);
    }


    public String toString() {
        return String.format("HashedWheelTimer { Buffer Size: %d, Resolution: %d }",
                wheelSize,
                resolution);
    }

    public void shutdown() {
        this.loop.shutdown();
    }


    public List<Runnable> shutdownNow() {
        return this.loop.shutdownNow();
    }


    public boolean isShutdown() {
        return this.loop.isShutdown();
    }


    public boolean isTerminated() {
        return this.loop.isTerminated();
    }


    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return this.loop.awaitTermination(timeout, unit);
    }

    /**
     * INTERNALS
     */

    private <V> Registration<V> scheduleOneShot(long firstDelay,
                                                Callable<V> callable, Executor executor) {
        assertRunning();
        isTrue(firstDelay >= resolution,
                "Cannot schedule tasks for amount of time less than timer precision.");
        int firstFireOffset = (int) (firstDelay / resolution);
        int firstFireRounds = firstFireOffset / wheelSize;

        Registration<V> r = new OneShotRegistration<V>(firstFireRounds, callable, firstDelay, executor);
        // We always add +1 because we'd like to keep to the right boundary of event on execution, not to the left:
        //
        // For example:
        //    |          now          |
        // res start               next tick
        // The earliest time we can tick is aligned to the right. Think of it a bit as a `ceil` function.
        wheel[idx(cursor + firstFireOffset + 1)].add(r);
        return r;
    }

    private <V> Registration<V> scheduleFixedRate(long recurringTimeout,
                                                  long firstDelay,
                                                  Callable<V> callable,Executor executor) {
        assertRunning();
        isTrue(recurringTimeout >= resolution,
                "Cannot schedule tasks for amount of time less than timer precision.");

        int offset = (int) (recurringTimeout / resolution);
        int rounds = offset / wheelSize;

        int firstFireOffset = (int) (firstDelay / resolution);
        int firstFireRounds = firstFireOffset / wheelSize;

        Registration<V> r = new FixedRateRegistration<>(firstFireRounds, callable, recurringTimeout,executor, rounds, offset);
        wheel[idx(cursor + firstFireOffset + 1)].add(r);
        return r;
    }

    private <V> Registration<V> scheduleFixedDelay(long recurringTimeout,
                                                   long firstDelay,
                                                   Callable<V> callable,Executor executor) {
        assertRunning();
        isTrue(recurringTimeout >= resolution,
                "Cannot schedule tasks for amount of time less than timer precision.");

        int offset = (int) (recurringTimeout / resolution);
        int rounds = offset / wheelSize;

        int firstFireOffset = (int) (firstDelay / resolution);
        int firstFireRounds = firstFireOffset / wheelSize;

        Registration<V> r = new FixedDelayRegistration<>(firstFireRounds, callable, recurringTimeout,executor, rounds, offset,
                this::reschedule);
        wheel[idx(cursor + firstFireOffset + 1)].add(r);
        return r;
    }

    /**
     * Rechedule a {@link Registration} for the next fire
     */
    private void reschedule(Registration<?> registration) {
        registration.reset();
        wheel[idx(cursor + registration.getOffset() + 1)].add(registration);
    }

    private int idx(int cursor) {
        return cursor % wheelSize;
    }

    private void assertRunning() {
        if (this.loop.isTerminated()) {
            throw new IllegalStateException("Timer is not running");
        }
    }

    private static void isTrue(boolean expression, String message) {
        if (!expression) {
            throw new IllegalArgumentException(message);
        }
    }

    private static Callable<?> constantlyNull(Runnable r) {
        return () -> {
            r.run();
            return null;
        };
    }

}
