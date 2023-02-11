package org.protobj.mock.module.scene;

import com.guangyu.cd003.projects.gs.module.common.msg.RespIntSignedUniqInts;
import com.guangyu.cd003.projects.gs.module.legion.msg.RespLegion;
import com.guangyu.cd003.projects.gs.module.scene.msg.RespSceneElemt;
import com.guangyu.cd003.projects.gs.module.scene.msg.RespSceneOp;
import com.guangyu.cd003.projects.mock.module.legion.LegionData;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Set;

public class SceneData {
    public IntObjectMap<RespSceneElemt> respSceneElemtMap = new IntObjectHashMap<>();

    public void handle(MockConnect connect, RespSceneOp respSceneOp) {
        LegionData legion_data = connect.LEGION_DATA;
        legion_data.sceneLegions.clear();
        legion_data.deleteSixs.clear();
        if (CollectionUtils.isNotEmpty(respSceneOp.delElemt)) {
            for (RespIntSignedUniqInts respIntSignedUniqInts : respSceneOp.delElemt) {
                for (Integer integer : respIntSignedUniqInts.value) {
                    respSceneElemtMap.remove(integer.intValue());
                    for (RespLegion legion : legion_data.legions) {
                        if (integer == legion.six) {
                            legion_data.deleteSixs.add(legion.six);
                        }
                    }
                }
            }
        }
        Set<RespSceneElemt> updElemt = respSceneOp.updElemt;
        if (CollectionUtils.isNotEmpty(updElemt)) {
            for (RespSceneElemt respSceneElemt : updElemt) {
                respSceneElemtMap.remove(respSceneElemt.six);
                respSceneElemtMap.put(respSceneElemt.six, respSceneElemt);
                for (RespLegion legion : legion_data.legions) {
                    if (respSceneElemt.six == legion.six) {
                        legion_data.sceneLegions.add(respSceneElemt.as());
                    }
                }
            }
        }
        if (!legion_data.deleteSixs.isEmpty() || !legion_data.sceneLegions.isEmpty()) {
            legion_data.update(legion_data.deleteSixs, legion_data.sceneLegions);
        }

    }
}
