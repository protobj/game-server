package io.protobj.scheduler;

import java.util.concurrent.DelayQueue;

/**
 * 多层时间轮
 */
public class TimeWheel {
    private final int tick;
    private final int wheelSize;
    private final int aroundTs;
    private volatile long currentTimeMillis;

    private final Bucket[] buckets;
    private final DelayQueue<Bucket> delayQueue;
    private volatile TimeWheel overflowTimeWheel = null;

    public TimeWheel(int tick, int wheelSize, DelayQueue<Bucket> delayQueue) {
        this.tick = tick;
        this.wheelSize = wheelSize;
        this.aroundTs = this.tick * this.wheelSize;
        long curTs = HashedWheelTimer.getCurrentMilliSecond();
        this.currentTimeMillis = curTs - curTs % tick;
        this.buckets = new Bucket[wheelSize];
        for (int i = 0; i < buckets.length; i++) {
            buckets[i] = new Bucket();
        }
        this.delayQueue = delayQueue;
    }

    public boolean addTask(TimedTask<?> task) {
        long expTs = task.expireTimeMillis();
        if (task.isCancelled()) {
            return false;
        }
        if (expTs < currentTimeMillis + tick) {
            return false;
        } else if (expTs < currentTimeMillis + aroundTs) {
            long c = expTs / tick;
            int idx = (int) (c % wheelSize);
            Bucket bkt = buckets[idx];
            bkt.add(task);
            if (bkt.setExpireTimeMillis(c * tick)) {
                //bucket第一次有数据，放入queue中
                delayQueue.offer(bkt);
            }
            return true;
        } else {
            //超过本轮范围，递归交给上级
            if (overflowTimeWheel == null) {
                addOverflowWheel();
            }
            return overflowTimeWheel.addTask(task);
        }
    }

    public void advanceClock(long expireTimeMillis) {
        //推进
        if (expireTimeMillis >= currentTimeMillis + tick) {
            currentTimeMillis = expireTimeMillis - expireTimeMillis % tick;
            if (overflowTimeWheel != null) {
                overflowTimeWheel.advanceClock(expireTimeMillis);
            }
        }
    }

    private void addOverflowWheel() {
        synchronized (this) {
            if (overflowTimeWheel == null) {
                overflowTimeWheel = new TimeWheel(wheelSize, tick * wheelSize, delayQueue);
            }
        }
    }

    public int getTick() {
        return tick;
    }
}