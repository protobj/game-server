package org.protobj.mock.module.city;

import com.guangyu.cd003.projects.common.cons.Commands;
import com.guangyu.cd003.projects.gs.module.city.cfg.BuildingCfg;
import com.guangyu.cd003.projects.gs.module.city.msg.RqstCityCreBuilding;
import com.guangyu.cd003.projects.gs.module.common.vo.PosShort;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.pv.framework.gs.core.cfg.core.CacheConfig;
import com.pv.framework.gs.core.util.RandomUtils;

import java.util.List;

public enum AddBuildingState {
    //尝试建造
    IDLE() {
        @Override
        public void execute(MockConnect connect) {
            CityData city_data = connect.CITY_DATA;
            boolean hasFreeWorker = city_data.findFreeWorker();
            if (!hasFreeWorker) {
                city_data.setAddBuildingState(RQST_CRE_WORKER);
                return;
            }
            List<Integer> canCreBuildingCids = city_data.canCreBuildingCids;
            if (canCreBuildingCids.isEmpty()) {
                city_data.resetCanCreBuildings();
            }
            if (canCreBuildingCids.isEmpty()) {
                city_data.setAddBuildingState(NOT_BUILDING_TO_BUILD);
                return;
            }
            Integer cid = canCreBuildingCids.remove(0);
            RqstCityCreBuilding rqstCityCreBuilding = new RqstCityCreBuilding();
            BuildingCfg cfg = CacheConfig.getCfg(BuildingCfg.class, cid);
            PosShort freePit = city_data.findFreePit(cfg);
            if (freePit == null) {
                city_data.setAddBuildingState(AddBuildingState.NOT_FREE_PIT);
                return;
            }
            rqstCityCreBuilding.cid = cid;
            rqstCityCreBuilding.immediate = RandomUtils.nextInt(10000) < 3000;
            rqstCityCreBuilding.x = freePit.getX();
            rqstCityCreBuilding.z = freePit.getZ();
            connect.send(Commands.CITY_CRE_BUILD_CONST, rqstCityCreBuilding).whenCompleteAsync((r, e) -> {
                if (e != null) {
                    connect.CITY_DATA.setAddBuildingState(AddBuildingState.IDLE);
                }
            });
            connect.CITY_DATA.setAddBuildingState(DOING);
        }
    },
    //建造中
    DOING {
        @Override
        public void execute(MockConnect connect) {

        }
    },
    //请求续费工人
    RQST_CRE_WORKER() {
        @Override
        public void execute(MockConnect connect) {
            CityData city_data = connect.CITY_DATA;
            int i = city_data.needPayWorkerCount();
            if (i > 0) {
                city_data.setAddBuildingState(AddBuildingState.RQST_CRE_WORKER_ING);
                connect.send(Commands.CITY_CITY_CHANGE_WORKER_CONST, null).whenCompleteAsync((r, e) -> {
                    if (e != null) {
                        city_data.setAddBuildingState(AddBuildingState.NOT_WORKER_FREE);
                    }
                }, connect.executor());
            } else {
                city_data.setAddBuildingState(AddBuildingState.NOT_WORKER_FREE);
            }
        }
    },
    //放不下建筑了
    NOT_FREE_PIT,
    //没有工人可用
    NOT_WORKER_FREE{
        @Override
        public void execute(MockConnect connect) {
            super.execute(connect);
        }
    },
    //没有建筑可建造
    NOT_BUILDING_TO_BUILD,
    //正在请求创建worker
    RQST_CRE_WORKER_ING,

    ;

    public void execute(MockConnect connect) {

    }
}
