package io.protobj.thread;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class CustomThreadFactory implements ThreadFactory {

    private static final AtomicInteger poolNumber = new AtomicInteger(1);

    protected final ThreadGroup group;
    protected final AtomicInteger threadNumber = new AtomicInteger(1);
    protected final String namePrefix;

    public CustomThreadFactory(ThreadGroup threadGroup, String name) {
        this.group = threadGroup;
        namePrefix = "pool-" + name + "-" + poolNumber.getAndIncrement() + "-thread-";
    }

    public static CustomThreadFactory create(ThreadGroup threadGroup, String name) {
        return new CustomThreadFactory(threadGroup, name);
    }

    public Thread newThread(Runnable r) {
        Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
        if (t.isDaemon())
            t.setDaemon(false);
        if (t.getPriority() != Thread.NORM_PRIORITY)
            t.setPriority(Thread.NORM_PRIORITY);
        return t;
    }
}
