package io.protobj.thread;

import org.apache.commons.lang3.RandomUtils;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class ExecutorGroup implements Executor {

    private final ExecutorService[] executors;
    private final AtomicInteger nextIndex = new AtomicInteger();

    public ExecutorGroup(ThreadGroup threadGroup, String name, int size) {
        CustomThreadFactory factory = new CustomThreadFactory(threadGroup, name);
        this.executors = new ExecutorService[size];
        for (int i = 0; i < size; i++) {
            this.executors[i] = Executors.newSingleThreadExecutor(factory);
        }
    }

    public Executor next() {
        return executors[nextIndex.getAndIncrement() % executors.length];
    }

    public Executor random() {
        return executors[RandomUtils.nextInt(0, executors.length)];
    }

    public void shutdown() {
        for (ExecutorService scheduledExecutorService : executors) {
            scheduledExecutorService.shutdownNow();
        }
    }

    @Override
    public void execute(Runnable command) {
        random().execute(command);
    }

}
