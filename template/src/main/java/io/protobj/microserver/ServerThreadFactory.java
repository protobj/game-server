package io.protobj.microserver;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerThreadFactory implements ThreadFactory {

    private static final AtomicInteger poolNumber = new AtomicInteger(1);

    protected final ThreadGroup group;
    protected final AtomicInteger threadNumber = new AtomicInteger(1);
    protected final String namePrefix;

    public ServerThreadFactory(String groupName, String name) {
        if (groupName!=null && !"".equals(groupName))
            this.group = new ThreadGroup(groupName);
        else {
            @SuppressWarnings("removal")
            SecurityManager s = System.getSecurityManager();

            if (s != null){
                @SuppressWarnings("removal")
                ThreadGroup threadGroup = s.getThreadGroup();
                this.group = threadGroup;
            }else{
                this.group =  Thread.currentThread().getThreadGroup();
            }
        }
        namePrefix = "pool-" + name + "-" + poolNumber.getAndIncrement() + "-thread-";
    }

    public static ServerThreadFactory create(String groupName, String name) {
        return new ServerThreadFactory(groupName, name);
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
