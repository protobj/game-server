package org.protobj.mock.module.legion;

import com.guangyu.cd003.projects.gs.module.city.cfg.DscrDataCity;
import com.guangyu.cd003.projects.gs.module.leagueSeizeble.msg.RespPassDoorCantPass;
import com.guangyu.cd003.projects.gs.module.legion.cons.LegionActionType;
import com.guangyu.cd003.projects.gs.module.legion.msg.*;
import com.guangyu.cd003.projects.gs.module.legion.msg.state.RespLegionStatePathMv;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.pv.common.utilities.common.CommonUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import java.util.*;

/**
 * 服务器可能在某些操作时没有同步legion
 * 也可能时客户端丢包
 */
public class LegionData {
    public Set<RespLegion> legions = CommonUtil.createSet();
    public Set<RespMemberLegion> mLegions = CommonUtil.createSet();

    //副将解锁没
    public int maxQueueSize = 0;

    public int onCreCount = 0;//正在创建的个数
    //
    //key:rix
    public Map<Integer, LegionState> legionStates = CommonUtil.createSimpleMap();

    public List<RespSceneElemtLegion> sceneLegions = new ArrayList<>();
    public List<Integer> deleteSixs = new ArrayList<>();

    private void update() {
        for (RespLegion legion : legions) {
            LegionState state = legionStates.computeIfAbsent(legion.uniqIxOfHolder, t -> LegionState.newLegionState());
            state.update(legion);
        }
    }

    public void update(List<Integer> deleteSixs, List<RespSceneElemtLegion> sceneLegions) {
        boolean updated = legions.removeIf(next -> deleteSixs.contains(next.six));
        for (RespSceneElemtLegion sceneLegion : sceneLegions) {
            for (RespLegion legion : legions) {
                if (sceneLegion.six == legion.six) {
                    legion.spd = sceneLegion.spd;
                    legion.actionType = sceneLegion.actionType;
                    legion.state = sceneLegion.state;
                    legion.units = sceneLegion.units;
                    legion.x = sceneLegion.x;
                    legion.z = sceneLegion.z;
                    if (sceneLegion.state instanceof RespLegionStatePathMv) {
                        int pathIx = ((RespLegionStatePathMv) sceneLegion.state).pathIx;
                        List<Integer> path = ((RespLegionStatePathMv) sceneLegion.state).path;
                        legion.x = path.get(pathIx);
                        legion.z = path.get(pathIx + 1);
                    }
                    updated = true;
                    break;
                }
            }
        }
        if (updated) {
            update();
        }
    }

    private void delete(Set<Integer> deletes) {
        for (Integer delete : deletes) {
            LegionState remove = legionStates.remove(delete);
            if (remove != null) {
                remove.setState(LegionActionType.RETURN.getType());
                remove.recycle();
            }
        }
    }

    public void resetQueueSize(MockConnect connect) {
        int lv = connect.CITY_DATA.respCityInfo.lv;
        this.maxQueueSize = getLegionQueueSize(lv);
    }

    private int getLegionQueueSize(int lv) {
        int[] data = DscrDataCity.LEGION_QUEUE_MAX_LIMIT.getData();
        int dataIx = Math.min(lv, data.length - 1);
        return data[dataIx];
    }

    public int unitCount(RespLegion respLegion) {
        int count = 0;
        for (Integer value : respLegion.units.values()) {
            count += value;
        }
        return count;
    }

    public int initUnitCount(RespLegion respLegion) {
        int count = 0;
        for (Integer value : respLegion.initUnits.values()) {
            count += value;
        }
        return count;
    }

    public void handle(RespLegionOp respLegionOp) {

        if (MapUtils.isNotEmpty(respLegionOp.sceneLegionActionUpd)) {
            respLegionOp.sceneLegionActionUpd.forEach((six, action) -> {
                for (RespLegion legion : legions) {
                    if (legion.six == six) {
                        legion.actionType = action;
                    }
                }
            });
        }

        if (CollectionUtils.isNotEmpty(respLegionOp.updLegions)) {
            legions.removeIf(it -> respLegionOp.updLegions.stream().anyMatch(t -> t.uniqIxOfHolder == it.uniqIxOfHolder));
            legions.addAll(respLegionOp.updLegions);
        }

        if (MapUtils.isNotEmpty(respLegionOp.sceneLegionStateUpd)) {
            respLegionOp.sceneLegionStateUpd.forEach((six, state) -> {
                for (RespLegion legion : legions) {
                    if (legion.six == six) {
                        legion.state = state;
                    }
                }
            });
        }

        if (MapUtils.isNotEmpty(respLegionOp.sceneLegionUnitUpd)) {
            respLegionOp.sceneLegionUnitUpd.forEach((six, unitMap) -> {
                for (RespLegion legion : legions) {
                    if (legion.six == six) {
                        legion.units = unitMap.units;
                        legion.spd = unitMap.spd;
                    }
                }
            });
        }
        if (CollectionUtils.isNotEmpty(respLegionOp.rmLegionRixs)) {
            legions.removeIf(it -> respLegionOp.rmLegionRixs.contains(it.uniqIxOfHolder));
            delete(respLegionOp.rmLegionRixs);
        }

        if (MapUtils.isNotEmpty(respLegionOp.sceneLegionSpdUpd)) {
            respLegionOp.sceneLegionSpdUpd.forEach((six, spd) -> {
                for (RespLegion legion : legions) {
                    if (legion.six == six) {
                        legion.spd = spd;
                    }
                }
            });
        }
        update();
    }


    public void handle(RespLegionInfo respLegionInfo) {
        legions.clear();
        mLegions.clear();
        legionStates.clear();
        if (respLegionInfo.legions != null) {
            legions.addAll(respLegionInfo.legions);
        }
        if (respLegionInfo.mLegions != null) {
            mLegions.addAll(respLegionInfo.mLegions);
        }
        update();
    }

    public void handle(RespPassDoorCantPass respMsg) {
        for (LegionState legionState : legionStates.values()) {
            if (legionState.legion.six == respMsg.whosix) {
                legionState.setState(LegionActionType.WAIT.getType());
            }
        }
    }

    public int legionSize() {
        return legionStates.size();
    }


}
