package org.protobj.mock.module.barracks;

import com.guangyu.cd003.projects.gs.module.barracks.msg.RespBarracks;
import com.guangyu.cd003.projects.gs.module.barracks.msg.RespBarracksInfo;
import com.guangyu.cd003.projects.gs.module.barracks.msg.RespBarracksOp;
import com.guangyu.cd003.projects.gs.module.common.msg.RespTimeQuantify;
import com.pv.common.utilities.common.CommonUtil;

import java.util.HashMap;
import java.util.Map;

public class BarracksData {


    Map<Integer, RespBarracks> barracks;

    Map<Integer, BarracksState> stateMap;

    public void handle(RespBarracksInfo respBarracksInfo) {
        Map<Integer, RespBarracks> barracksMap = respBarracksInfo.barracks;
        if (barracksMap != null) {
            this.barracks = barracksMap;
        } else {
            this.barracks = CommonUtil.createSimpleMap();
        }
        stateMap = new HashMap<>();
        stateChange();
    }

    public void stateChange() {
        barracks.forEach((id, barrack) -> {
            resetState(barrack);
        });
    }

    public BarracksState getBarracksState(Integer id) {
        return stateMap.computeIfAbsent(id, t -> new BarracksState());
    }

    public void handle(RespBarracksOp respBarracksOp) {
        if (respBarracksOp.barracksUpd != null) {
            barracks.putAll(respBarracksOp.barracksUpd);
        }
        stateChange();
    }

    public void resetState(RespBarracks barrack) {
        int curSec = (int) (System.currentTimeMillis() / 1000);
        BarracksState barracksState = getBarracksState(barrack.buildingRix);
        RespTimeQuantify trainEndTime = barrack.trainEndTime;
        if (trainEndTime.endSec <= 0) {
            barracksState.setCurState(BarracksState.IDLE);
        } else if (trainEndTime.endSec > curSec) {
            barracksState.setCurState(BarracksState.TRAINING);
        } else {
            barracksState.setCurState(BarracksState.TRAIN_COMPLETE);
        }
    }
}
