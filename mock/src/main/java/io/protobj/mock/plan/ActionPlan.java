package io.protobj.mock.plan;

import io.protobj.mock.net.MockConnect;
import io.reactivex.rxjava3.core.Observable;
import org.apache.commons.lang3.RandomUtils;

import java.util.concurrent.TimeUnit;

public class ActionPlan extends Plan {
    public ActionPlan() {
        for (ActionType value : ActionType.values()) {
            value.setInterval(new int[]{30, 30});
        }
    }

    @Override
    protected Observable<Integer> execute0(MockConnect connect) {
//        for (ActionType value : ActionType.values()) {
//            ActionRunnable actionRunnable = new ActionRunnable(connect, value);
//            actionRunnable.run();
//        }

        connect.getScheduler().schedulePeriodicallyDirect(() -> {
            if (connect.isOffLine()) {
                return;
            }
            for (ActionType value : ActionType.values()) {
                value.execute(connect);
            }
        }, 0, 4, TimeUnit.SECONDS);
        return Observable.empty();
    }

    public static class ActionRunnable implements Runnable {
        MockConnect connect;
        ActionType actionType;

        public ActionRunnable(MockConnect connect, ActionType value) {
            this.connect = connect;
            this.actionType = value;
        }

        @Override
        public void run() {
            if (connect.isOffLine()) {
                return;
            }
            actionType.execute(connect);
            connect.getScheduler().scheduleDirect(this, RandomUtils.nextInt(actionType.interval[0], actionType.interval[1] + 1), TimeUnit.SECONDS);
        }
    }


    public enum ActionType {
        changeCityOrScene(new int[]{3, 6}) {
            @Override
            public void execute(MockConnect connect) {
            }
        }, train(new int[]{3, 6}) {
            @Override
            public void execute(MockConnect connect) {

            }
        }, chat(new int[]{3, 6}) {
            @Override
            public void execute(MockConnect connect) {

            }
        }, watchCity(new int[]{3, 6}) {
            @Override
            public void execute(MockConnect connect) {

            }
        }, productCollect(new int[]{3, 6}) {
            @Override
            public void execute(MockConnect connect) {

            }
        }, upgradeBuilding(new int[]{3, 6}) {
            @Override
            public void execute(MockConnect connect) {

            }
        }, creBuilding(new int[]{3, 6}) {
            @Override
            public void execute(MockConnect connect) {

            }
        }, mail(new int[]{3, 6}) {
            @Override
            public void execute(MockConnect connect) {

            }
        }, useItem(new int[]{3, 6}) {
            @Override
            public void execute(MockConnect connect) {

            }
        }, joinOrCreLeague(new int[]{3, 6}) {
            @Override
            public void execute(MockConnect connect) {

            }
        }, legion(new int[]{1, 3}) {
            @Override
            public void execute(MockConnect connect) {

            }
        }, cure(new int[]{2, 4}) {
            @Override
            public void execute(MockConnect connect) {

            }
        };

        private int[] interval;

        ActionType(int[] interval) {
            this.interval = interval;
        }

        public int[] getInterval() {
            return interval;
        }

        public void setInterval(int[] interval) {
            this.interval = interval;
        }

        public abstract void execute(MockConnect connect);
    }

}
