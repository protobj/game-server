package org.protobj.mock.module.scene.selector;

import com.guangyu.cd003.projects.gs.module.city.msg.RespCanAtkNpcLv;
import com.guangyu.cd003.projects.gs.module.city.msg.RespCityInfo;
import com.guangyu.cd003.projects.gs.module.legion.msg.RespSceneElemtNpcLegion;
import com.guangyu.cd003.projects.gs.module.npc.cfg.NpcLvGroupCfg;
import com.guangyu.cd003.projects.gs.module.npc.cons.NpcType;
import com.guangyu.cd003.projects.gs.module.scene.cfg.SceneRecastCfg;
import com.guangyu.cd003.projects.gs.module.scene.msg.RespSceneElemt;
import com.guangyu.cd003.projects.mock.module.scene.RespSceneElemtSelector;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.pv.framework.gs.core.cfg.core.CacheConfig;
import com.pv.framework.gs.core.util.RandomUtils;

import java.util.List;

public class NpcRespSceneElemtSelector implements RespSceneElemtSelector {

    public static final NpcRespSceneElemtSelector MONSTER = new NpcRespSceneElemtSelector(NpcType.Scene);
    public static final NpcRespSceneElemtSelector BUILDING = new NpcRespSceneElemtSelector(NpcType.Building);
    public static final NpcRespSceneElemtSelector HUNT = new NpcRespSceneElemtSelector(NpcType.Hunt);

    NpcType npcType;

    private NpcRespSceneElemtSelector(NpcType npcType) {
        this.npcType = npcType;
    }

    public NpcType getNpcType() {
        return npcType;
    }

    @Override
    public RespSceneElemt select(MockConnect connect, List<RespSceneElemt> elemts) {
        return RandomUtils.random(elemts);
    }

    private int getCanAtkLv(MockConnect connect) {
        RespCityInfo respCityInfo = connect.CITY_DATA.respCityInfo;
        int cityRegionId = connect.ROLE_DATA.respRoleInfo.roleInfo.cityRegionId;
        int mapId = CacheConfig.getCfg(SceneRecastCfg.class, cityRegionId).getMapId();
        RespCanAtkNpcLv respCanAtkNpcLv = respCityInfo.canAtkNpcLvMap.get(mapId);
        if (respCanAtkNpcLv == null) {
            return 1;
        }
        return respCanAtkNpcLv.npcTypeLv.getOrDefault(npcType.getCode(), 0) + 1;
    }

    private NpcLvGroupCfg getCfg(RespSceneElemt o) {
        return CacheConfig.getCfg(NpcLvGroupCfg.class, o.as(RespSceneElemtNpcLegion.class).npcLvId);
    }

    @Override
    public boolean filter(MockConnect mockConnect, RespSceneElemt elemt) {
        if (!(elemt instanceof RespSceneElemtNpcLegion)) {
            return false;
        }
        RespSceneElemtNpcLegion as = elemt.as();
        int npcLvId = as.npcLvId;
        NpcLvGroupCfg cfg = CacheConfig.getCfg(NpcLvGroupCfg.class, npcLvId);
        return cfg.getNpcType() == npcType && getCanAtkLv(mockConnect) >= cfg.getLv();
    }

}
