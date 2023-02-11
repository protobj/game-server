package org.protobj.mock.module.city;

import com.guangyu.cd003.projects.common.cons.Commands;
import com.guangyu.cd003.projects.gs.module.city.cfg.BuildingCfg;
import com.guangyu.cd003.projects.gs.module.city.cons.SceneElemtTypeCity;
import com.guangyu.cd003.projects.gs.module.city.msg.RespSceneElemtCity;
import com.guangyu.cd003.projects.gs.module.city.msg.RqstCityCreBuilding;
import com.guangyu.cd003.projects.gs.module.city.msg.RqstCityLvUpBuilding;
import com.guangyu.cd003.projects.gs.module.city.msg.RqstCityQry;
import com.guangyu.cd003.projects.gs.module.common.vo.PosShort;
import com.guangyu.cd003.projects.mock.module.scene.SceneController;
import com.guangyu.cd003.projects.mock.module.scene.selector.CityRespSceneElemtSelector;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.pv.framework.gs.core.cfg.core.CacheConfig;
import com.pv.framework.gs.core.util.RandomUtils;

import java.util.List;

public class CityController {

    public static void watchCity(MockConnect mockConnect) {
        RespSceneElemtCity elemt = (RespSceneElemtCity) SceneController.findElemt(mockConnect, CityRespSceneElemtSelector.INSTANCE);
        if (elemt == null) {
            return;
        }
        RqstCityQry rqstCityQry = new RqstCityQry();
        rqstCityQry.rid = elemt.holderId;
        mockConnect.send(Commands.CITY_QRY_CONST, rqstCityQry);
    }


    public static void upgradeBuilding(MockConnect mockConnect) {
        CityData city_data = mockConnect.CITY_DATA;
        boolean freeWorker = mockConnect.CITY_DATA.findFreeWorker();
        if (!freeWorker) {
            return;
        }
        List<Integer> canUpgradeBuildingRix = city_data.canUpgradeBuildingRix;
        if (canUpgradeBuildingRix.isEmpty()) {
            city_data.resetCanUpgradeBuilding();
        }
        if (canUpgradeBuildingRix.isEmpty()) {
            return;
        }
        Integer remove = canUpgradeBuildingRix.remove(0);
        RqstCityLvUpBuilding rqstCityLvUpBuilding = new RqstCityLvUpBuilding();
        rqstCityLvUpBuilding.rix = remove;
        rqstCityLvUpBuilding.immediate = RandomUtils.nextInt(10000) < 3000;
        mockConnect.send(Commands.CITY_LVUP_BUILD_CONST, rqstCityLvUpBuilding);
    }

    public static void addBuilding(MockConnect mockConnect) {
        CityData city_data = mockConnect.CITY_DATA;
        city_data.addBuildingState.execute(mockConnect);
    }
}
