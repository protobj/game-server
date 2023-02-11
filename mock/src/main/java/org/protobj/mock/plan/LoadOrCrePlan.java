package org.protobj.mock.plan;

import com.guangyu.cd003.projects.mock.module.role.RoleController;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import io.reactivex.rxjava3.core.Observable;

import java.util.concurrent.atomic.AtomicLong;

public class LoadOrCrePlan extends Plan {

    public static final AtomicLong onlineCount = new AtomicLong();
    public static AtomicLong startTime = new AtomicLong();

    public static volatile AtomicLong endTime = new AtomicLong();

    public LoadOrCrePlan() {
        startTime.compareAndSet(0, System.currentTimeMillis());
    }

    @Override
    protected Observable<Integer> execute0(MockConnect connect) {
        Observable<Integer> future = RoleController.loadOrCreateRole(connect);
        return future.concatWith(Observable.fromRunnable(() -> {
            onlineCount.incrementAndGet();
            endTime.getAndSet(System.currentTimeMillis());
        }));
    }
}
