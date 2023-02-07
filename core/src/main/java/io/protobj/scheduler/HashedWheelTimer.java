package io.protobj.scheduler;

import io.protobj.thread.CustomThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.support.CronExpression;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class HashedWheelTimer implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(HashedWheelTimer.class);

    public interface ExpireCallback {
        void onTime(TimedTask<?> task);
    }

    private final AtomicLong taskCount = new AtomicLong(0L);
    private final DelayQueue<Bucket> delayQueue = new DelayQueue<Bucket>();

    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    private TimeWheel timeWheel;

    private final ZoneId zoneId;

    private final ExecutorService loop;

    public HashedWheelTimer(ThreadGroup threadGroup, int tickMilliSecond, int wheelSize, ZoneId zoneId) {
        this.zoneId = zoneId != null ? zoneId : ZoneId.systemDefault();
        this.timeWheel = new TimeWheel(tickMilliSecond, wheelSize, delayQueue);
        this.loop = Executors.newSingleThreadExecutor(CustomThreadFactory.create(threadGroup, HashedWheelTimer.class.getSimpleName()));
        this.loop.submit(this);
    }

    public void shutdown() {
        this.loop.shutdownNow();
    }

    public void add(TimedTask<?> task) {
        boolean ok;
        rwLock.readLock().lock();
        try {
            ok = this.timeWheel.addTask(task);
        } finally {
            rwLock.readLock().unlock();
        }
        if (ok) {
            taskCount.incrementAndGet();
        } else if (!task.isCancelled()) {
            task.tryRepeatOnExecute(this::add, zoneId);
            task.executor().execute(task);
        }
    }

    public long size() {
        return taskCount.get();
    }

    protected void onShutdown() {
        taskCount.set(0);
        delayQueue.clear();
        timeWheel = null;
    }

    public static long getCurrentMilliSecond() {
        return System.currentTimeMillis();
    }

    private void flush(TimedTask<?> task) {
        //最小轮添加会失败，进入submit
        //其它轮超时后，会迁移刷新到小轮次中
        boolean ok = this.timeWheel.addTask(task);
        if (ok) {
            taskCount.incrementAndGet();
        } else if (task.isCancelled()) {
            taskCount.decrementAndGet();
        } else {
            //已经超时
            taskCount.decrementAndGet();
            task.tryRepeatOnExecute(this::add, zoneId);
            task.executor().execute(task);
        }
    }


    @Override
    final public void run() {
        try {
            logger.info("thread start: " + Thread.currentThread().getName());
            process();
        } finally {
            onShutdown();
            logger.info("thread shutDown: " + Thread.currentThread().getName());
        }
    }

    /**
     * 处理器
     */
    public void process() {
        while (true) {
            try {
                Bucket bucket = delayQueue.take();
                //lock ==> 时间轮滚动的时候不能添加任务
                rwLock.writeLock().lock();
                try {
                    timeWheel.advanceClock(bucket.getExpireTimeMillis());
                    bucket.expire(this::flush);
                } finally {
                    //unlock
                    rwLock.writeLock().unlock();
                }
            } catch (InterruptedException e) {
                logger.warn("interrupted: %s".formatted(Thread.currentThread().getName()), e);
                break;
            } catch (Exception e) {
                logger.error("error: %s".formatted(Thread.currentThread().getName()), e);
            }
        }
    }

    public TimedTask<Void> execute(Executor executor, Runnable runnable) {
        return execute(executor, constantlyNull(runnable));
    }

    public <T> TimedTask<T> execute(Executor executor, Callable<T> callable) {
        OneShotTimedTask<T> task = new OneShotTimedTask<>(0, executor, callable);
        add(task);
        return task;
    }

    public TimedTask<Void> fixedRate(Executor executor, long period, Runnable runnable) {
        return fixedRate(executor, period, constantlyNull(runnable));
    }

    public <T> TimedTask<T> fixedRate(Executor executor, long period, Callable<T> callable) {
        long curTs = getCurrentMilliSecond();
        long currentTimeMillis = curTs - curTs % timeWheel.getTick();
        FixedRateTimedTask<T> task = new FixedRateTimedTask<>(currentTimeMillis, executor, callable, period);
        add(task);
        return task;
    }

    public TimedTask<Void> fixedDelay(Executor executor, long period, Runnable runnable) {

        return fixedDelay(executor, period, constantlyNull(runnable));
    }

    public <T> TimedTask<T> fixedDelay(Executor executor, long period, Callable<T> callable) {
        long curTs = getCurrentMilliSecond();
        long currentTimeMillis = curTs - curTs % timeWheel.getTick();
        FixedDelayTimedTask<T> task = new FixedDelayTimedTask<>(currentTimeMillis, executor, callable, period, this::add);
        add(task);
        return task;
    }

    public TimedTask<Void> cron(Executor executor, String cron, Runnable runnable) {
        return cron(executor, cron, constantlyNull(runnable));
    }

    public <T> TimedTask<T> cron(Executor executor, String cron, Callable<T> callable) {
        long curTs = getCurrentMilliSecond();
        long currentTimeMillis = curTs - curTs % timeWheel.getTick();
        CronExpression cronExpression = CronExpression.parse(cron);
        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(currentTimeMillis), zoneId);
        ZonedDateTime next = cronExpression.next(zonedDateTime);
        if (next == null) {
            throw new IllegalArgumentException("cron：%s error".formatted(cron));
        }
        CronTimedTask<T> task = new CronTimedTask<>(next.toInstant().toEpochMilli(), executor, callable, cronExpression);
        add(task);
        return task;
    }

    public long getTick() {
        return timeWheel.getTick();
    }

    private static <T> Callable<T> constantlyNull(Runnable r) {
        return () -> {
            r.run();
            return null;
        };
    }
}