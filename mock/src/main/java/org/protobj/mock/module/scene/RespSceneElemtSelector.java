package org.protobj.mock.module.scene;

import com.guangyu.cd003.projects.gs.module.scene.msg.RespSceneElemt;
import com.guangyu.cd003.projects.mock.net.MockConnect;

import java.util.List;

public interface RespSceneElemtSelector {
    default RespSceneElemt select(MockConnect connect, List<RespSceneElemt> elemts) {
        int x = connect.CITY_DATA.respCityInfo.x;
        int z = connect.CITY_DATA.respCityInfo.z;
        elemts.sort((o1, o2) -> ((int) (Math.pow(o1.x / 100d - x, 2) + Math.pow(o1.z / 100d - z, 2)
                - Math.pow(o2.x / 100d - x, 2) - Math.pow(o2.z / 100d - z, 2))));
        return elemts.get(0);
    }

    boolean filter(MockConnect mockConnect, RespSceneElemt elemt);
}
