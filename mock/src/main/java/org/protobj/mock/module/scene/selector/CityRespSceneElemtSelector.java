package org.protobj.mock.module.scene.selector;

import com.guangyu.cd003.projects.gs.module.city.msg.RespSceneElemtCity;
import com.guangyu.cd003.projects.gs.module.scene.msg.RespSceneElemt;
import com.guangyu.cd003.projects.mock.module.scene.RespSceneElemtSelector;
import com.guangyu.cd003.projects.mock.net.MockConnect;

public class CityRespSceneElemtSelector implements RespSceneElemtSelector {

    public static final RespSceneElemtSelector INSTANCE = new CityRespSceneElemtSelector();

    private CityRespSceneElemtSelector() {
    }

    @Override
    public boolean filter(MockConnect mockConnect, RespSceneElemt elemt) {
        if (!(elemt instanceof RespSceneElemtCity)) {
            return false;
        }
        RespSceneElemtCity city = (RespSceneElemtCity) elemt;
        if (city.cityShield) {
            return false;
        }
        if (city.holderId.equals(mockConnect.ROLE_DATA.respRoleInfo.roleInfo.id)) {
            return false;
        }
        return true;
    }
}
