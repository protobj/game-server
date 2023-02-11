package io.protobj.mock.plan;

import io.protobj.mock.net.MockConnect;
import io.reactivex.rxjava3.core.Observable;

/**
 * Created on 2021/5/20.
 *
 * @author chen qiang
 */
public abstract class Plan {

    protected int executeCount = 1;

    private int currentExecuteCount = 0;

    public Observable<Integer> execute(MockConnect connect) {
        return execute0(connect);
    }

    protected abstract Observable<Integer> execute0(MockConnect connect);


    public long getDelay() {
        return 100;
    }


    Plan nextPlan;

    public Plan nextPlan(Plan plan) {
        nextPlan = plan;
        return plan;
    }

    public Plan getNextPlan() {
        currentExecuteCount++;
        //满足执行次数后执行下一个
        if (!isDone()) {
            return this;
        }
        return nextPlan;
    }

    public boolean isDone() {
        return currentExecuteCount >= executeCount;
    }


}
