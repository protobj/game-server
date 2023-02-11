package org.protobj.mock.executor;

import com.pv.common.utilities.concurrent.threadpool.CustomThreadFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class ExecutorGroup {

    private final ScheduledExecutorService[] executorService;
    private final AtomicInteger nextIndex = new AtomicInteger();

    public ExecutorGroup(int size) {
        this.executorService = new ScheduledExecutorService[size];
        for (int i = 0; i < size; i++) {
            this.executorService[i] = Executors.newSingleThreadScheduledExecutor(new CustomThreadFactory("MockExecutorGroup"));
        }
    }


    public ScheduledExecutorService next() {
        return executorService[nextIndex.getAndIncrement() % executorService.length];
    }

    public void shutdown() {
        for (ScheduledExecutorService scheduledExecutorService : executorService) {
            scheduledExecutorService.shutdownNow();
        }
    }
}
