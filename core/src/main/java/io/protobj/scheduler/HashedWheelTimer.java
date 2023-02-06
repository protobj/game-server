package io.protobj.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.support.CronExpression;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.Callable;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class HashedWheelTimer implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(HashedWheelTimer.class);

    private volatile Thread currentThread = null;

    public interface ExpireCallback {
        void onTime(TimedTask<?> task);
    }

    private final AtomicLong taskCount = new AtomicLong(0L);
    private final DelayQueue<Bucket> delayQueue = new DelayQueue<Bucket>();
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private TimeWheel timeWheel;

    private final ZoneId zoneId;

    public HashedWheelTimer() {
        this.zoneId = ZoneId.systemDefault();
    }

    public void startWith(int tickMilliSecond, int wheelSize) {
        synchronized (this) {
            if (isRunning()) {
                return;
            }
            this.timeWheel = new TimeWheel(tickMilliSecond, wheelSize, delayQueue);
            startThread();
        }
    }

    public void shutdown() {
        stopThread();
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

    protected long onLoop(long currentTime) throws InterruptedException {
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
        return 0;
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
            logger.info("thread onStart: " + selfName());
            onStart();
            process();
        } finally {
            currentThread = null;
            onShutdown();
            logger.info("thread onShutdown: " + selfName());
        }
    }

    /**
     * 处理器
     */
    public void process() {
        long sleepMillis = 0;
        while (true) {
            try {
                sleepMillis = onLoop(System.currentTimeMillis());
                if (sleepMillis > 0) {
                    Thread.sleep(sleepMillis);
                }
            } catch (InterruptedException e) {
                logger.warn("thread isInterrupted: " + selfName(), e);
                break;
            } catch (Exception e) {
                logger.error("thread onLoop failed: " + selfName(), e);
            }
            if (currentThread.isInterrupted()) {
                logger.warn("thread isInterrupted: " + selfName());
                break;
            }
        }
    }

    protected void onStart() {
    }

    /**
     * 运行中
     *
     * @return
     */
    public boolean isRunning() {
        return currentThread != null;
    }

    /**
     * 启动
     */
    private void startThread() {
        synchronized (this) {
            if (currentThread == null) {
                currentThread = new Thread(this);
                currentThread.start();
            } else {
                logger.error("thread already started: " + selfName());
            }
        }
    }

    /**
     * 停止
     */
    private void stopThread() {
        synchronized (this) {
            if (currentThread != null) {
                currentThread.interrupt();
            }
        }
    }

    private String selfName() {
        return this.getClass().getSimpleName();
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

    private static <T> Callable<T> constantlyNull(Runnable r) {
        return () -> {
            r.run();
            return null;
        };
    }
}