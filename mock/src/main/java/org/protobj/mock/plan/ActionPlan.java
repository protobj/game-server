package org.protobj.mock.plan;

import com.guangyu.cd003.projects.mock.module.barracks.BarracksController;
import com.guangyu.cd003.projects.mock.module.chat.ChatController;
import com.guangyu.cd003.projects.mock.module.city.CityController;
import com.guangyu.cd003.projects.mock.module.depot.DepotController;
import com.guangyu.cd003.projects.mock.module.hospital.HospitalController;
import com.guangyu.cd003.projects.mock.module.league.LeagueController;
import com.guangyu.cd003.projects.mock.module.legion.LegionController;
import com.guangyu.cd003.projects.mock.module.mail.MailController;
import com.guangyu.cd003.projects.mock.module.product.ProductController;
import com.guangyu.cd003.projects.mock.module.scene.SceneController;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.pv.framework.gs.core.util.RandomUtils;
import io.reactivex.rxjava3.core.Observable;

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
            connect.getScheduler().scheduleDirect(this, RandomUtils.nextInt(actionType.interval), TimeUnit.SECONDS);
        }
    }


    public enum ActionType {
        changeCityOrScene(new int[]{3, 6}) {
            @Override
            public void execute(MockConnect connect) {
                int lv = RandomUtils.nextInt(5) + 1;
                int x = connect.CITY_DATA.respCityInfo.x;
                int z = connect.CITY_DATA.respCityInfo.z;
                SceneController.rqstElemt(connect, lv, x, z);
            }
        }, train(new int[]{3, 6}) {
            @Override
            public void execute(MockConnect connect) {
                BarracksController.train(connect);
            }
        }, chat(new int[]{3, 6}) {
            @Override
            public void execute(MockConnect connect) {
                ChatController.worldChat(connect);
            }
        }, watchCity(new int[]{3, 6}) {
            @Override
            public void execute(MockConnect connect) {
                CityController.watchCity(connect);
            }
        }, productCollect(new int[]{3, 6}) {
            @Override
            public void execute(MockConnect connect) {
                ProductController.collect(connect);
            }
        }, upgradeBuilding(new int[]{3, 6}) {
            @Override
            public void execute(MockConnect connect) {
                CityController.upgradeBuilding(connect);
            }
        }, creBuilding(new int[]{3, 6}) {
            @Override
            public void execute(MockConnect connect) {
                CityController.addBuilding(connect);
            }
        }, mail(new int[]{3, 6}) {
            @Override
            public void execute(MockConnect connect) {
                MailController.handleMail(connect);
            }
        }, useItem(new int[]{3, 6}) {
            @Override
            public void execute(MockConnect connect) {
                DepotController.use(connect);
            }
        }, joinOrCreLeague(new int[]{3, 6}) {
            @Override
            public void execute(MockConnect connect) {
                LeagueController.joinOrCre(connect);
            }
        }, legion(new int[]{1, 3}) {
            @Override
            public void execute(MockConnect connect) {
                LegionController.execute(connect);
            }
        }, cure(new int[]{2, 4}) {
            @Override
            public void execute(MockConnect connect) {
                HospitalController.cure(connect);
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
