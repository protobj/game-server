package org.protobj.mock.module.barracks;

import com.guangyu.cd003.projects.common.cons.Commands;
import com.guangyu.cd003.projects.gs.module.barracks.cfg.BarracksCfg;
import com.guangyu.cd003.projects.gs.module.barracks.cfg.BarracksCorpsInfoCfg;
import com.guangyu.cd003.projects.gs.module.barracks.msg.RespBarracks;
import com.guangyu.cd003.projects.gs.module.barracks.msg.RqstBarracksCollect;
import com.guangyu.cd003.projects.gs.module.barracks.msg.RqstBarracksTrain;
import com.guangyu.cd003.projects.gs.module.city.cfg.BuildingCdit;
import com.guangyu.cd003.projects.gs.module.city.cons.SyncFuncTypeCity;
import com.guangyu.cd003.projects.gs.module.common.cons.SpeedTimeType;
import com.guangyu.cd003.projects.gs.module.common.msg.RqstInts;
import com.guangyu.cd003.projects.gs.module.common.msg.RqstParamSet;
import com.guangyu.cd003.projects.gs.module.item.msg.RqstUseItem;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.pv.framework.gs.core.cfg.core.CacheConfig;
import com.pv.framework.gs.core.module.sys.vo.FuncConfig;
import com.pv.framework.gs.core.util.RandomUtils;
import io.netty.util.collection.IntObjectMap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class BarracksController {

    public static void train(MockConnect connect) {
        Map<Integer, RespBarracks> barracks = connect.BARRACKS_DATA.barracks;
        barracks.forEach((id, respBarracks) -> {
            try {
                CompletableFuture<Integer> future = proc(connect, connect.BARRACKS_DATA.getBarracksState(respBarracks.buildingRix), respBarracks);
                future.exceptionally(throwable -> {
                    connect.BARRACKS_DATA.resetState(respBarracks);
                    return null;
                });
            } catch (Exception e) {
                connect.BARRACKS_DATA.resetState(respBarracks);
            }
        });
    }

    private static CompletableFuture<Integer> proc(MockConnect connect, BarracksState barracksState, RespBarracks respBarracks) {
        int curState = barracksState.getCurState();
        if (curState == BarracksState.TRAINING) {
            int i = RandomUtils.nextInt(10000);
            if (i < 3000) { //30%会选择加速
                boolean speedup = RandomUtils.nextInt(2) == 1;
                if (speedup) {
                    barracksState.setCurState(BarracksState.RQST_SPEEDUP_ING);
                    return speedUp(connect, respBarracks);
                } else {
                    barracksState.setCurState(BarracksState.RQST_IMMEDIATE);
                    return trainImmediate0(connect, respBarracks);
                }
            }
        } else if (curState == BarracksState.TRAIN_COMPLETE) {
            barracksState.setCurState(BarracksState.RQST_COLLECT);
            return collect(connect, respBarracks);
        } else if (curState == BarracksState.IDLE) {
            barracksState.setCurState(BarracksState.RQST_TRAIN);
            return train0(connect, respBarracks, barracksState);
        } else if (curState == BarracksState.HAVE_NO_RSRC) {
            int cid = respBarracks.cid;
            BarracksCfg cfg = CacheConfig.getCfg(BarracksCfg.class, cid);
            int barracksType = cfg.getBarracksType();
            BarracksCorpsInfoCfg corpsInfoCfg = findBarracksCorpsInfoCfg(connect, barracksType);
            List<FuncConfig> rsrcCostsTypeCounts = corpsInfoCfg.getRsrcCostsTypeCounts();
            //判断资源是否足够
            boolean rsrcEnough = checkRsrcEnough(connect, rsrcCostsTypeCounts);
            if (rsrcEnough) {
                barracksState.setCurState(BarracksState.IDLE);
            }
        }
        return CompletableFuture.completedFuture(0);
    }

    private static CompletableFuture<Integer> trainImmediate0(MockConnect connect, RespBarracks respBarracks) {
        RqstBarracksTrain rqstBarracksTrain = new RqstBarracksTrain();
        rqstBarracksTrain.buildingRix = respBarracks.buildingRix;
        return connect.send(Commands.BARRACKS_TRAIN_IMMEDIATE_CONST, rqstBarracksTrain);
    }

    private static CompletableFuture<Integer> train0(MockConnect connect, RespBarracks respBarracks, BarracksState barracksState) {

        int cid = respBarracks.cid;
        BarracksCfg cfg = CacheConfig.getCfg(BarracksCfg.class, cid);
        int barracksType = cfg.getBarracksType();
        BarracksCorpsInfoCfg corpsInfoCfg = findBarracksCorpsInfoCfg(connect, barracksType);
        List<FuncConfig> rsrcCostsTypeCounts = corpsInfoCfg.getRsrcCostsTypeCounts();
        //判断资源是否足够
        boolean rsrcEnough = checkRsrcEnough(connect, rsrcCostsTypeCounts);
        if (!rsrcEnough) {
            barracksState.setCurState(BarracksState.HAVE_NO_RSRC);
            return CompletableFuture.completedFuture(0);
        }
        RqstBarracksTrain rqstBarracksTrain = new RqstBarracksTrain();
        rqstBarracksTrain.buildingRix = respBarracks.buildingRix;
        rqstBarracksTrain.trainLegionUnitId = corpsInfoCfg.getLegionUnit();
        rqstBarracksTrain.trainNum = cfg.getMaxTrains()[respBarracks.lv];
        rqstBarracksTrain.immediate = RandomUtils.nextInt(10000) < 2000;
        int cmd = Commands.BARRACKS_TRAIN_CONST;
        return connect.send(cmd, rqstBarracksTrain);
    }

    private static boolean checkRsrcEnough(MockConnect connect, List<FuncConfig> rsrcCostsTypeCounts) {
        for (FuncConfig rsrcCostsTypeCount : rsrcCostsTypeCounts) {
            if (SyncFuncTypeCity.CHG_RSRC_NUM.getType() == rsrcCostsTypeCount.getType()) {
                int param = rsrcCostsTypeCount.getParam();
                Long orDefault = connect.CITY_DATA.respCityInfo.resources.getOrDefault(param, 0L);
                if (rsrcCostsTypeCount.getValue() > orDefault) {
                    return false;
                }
            }
        }
        return true;
    }

    private static BarracksCorpsInfoCfg findBarracksCorpsInfoCfg(MockConnect connect, int barracksType) {
        IntObjectMap<BarracksCorpsInfoCfg> barracksCorpsInfoCfgMap = CacheConfig.getCfgsByClz(BarracksCorpsInfoCfg.class);
        List<BarracksCorpsInfoCfg> barracksCorpsInfoCfgs = barracksCorpsInfoCfgMap.values().stream().filter(it -> it.getBarracksType() == barracksType).filter(it -> {
            if (it.getUnlockTechId() > 0) {
                return false;
            }
            for (BuildingCdit buildingCdit : it.getBuildingCdits()) {
                Integer integer = connect.CITY_DATA.maxBuildingLv.get(buildingCdit.getType());
                if (integer == null) {
                    integer = 0;
                }
                if (integer < buildingCdit.getReqLv()) {
                    return false;
                }
            }
            return true;
        }).collect(Collectors.toList());
        BarracksCorpsInfoCfg corpsInfoCfg = RandomUtils.random(barracksCorpsInfoCfgs);
        return corpsInfoCfg;
    }


    private static CompletableFuture<Integer> collect(MockConnect connect, RespBarracks respBarracks) {
        RqstBarracksCollect rqstBarracksCollect = new RqstBarracksCollect();
        rqstBarracksCollect.buildingRix = respBarracks.buildingRix;
        return connect.send(Commands.BARRACKS_COLLECT_CONST, rqstBarracksCollect);
    }

    static List<Integer> trainSpeedItemIds = Arrays.stream(new int[]{107020201, 107020202, 107020203, 107020204, 107020205, 107020206, 107020207, 107020208, 107020209, 107020210, 107020211, 107020212, 107020213, 107020214, 107020215}).boxed().collect(Collectors.toList());

    private static CompletableFuture<Integer> speedUp(MockConnect connect, RespBarracks respBarracks) {
        RqstUseItem rqstUseItem = new RqstUseItem();
        Integer itemId = RandomUtils.random(trainSpeedItemIds);
        rqstUseItem.cid = itemId;
        rqstUseItem.num = 1;
        rqstUseItem.params = new RqstParamSet();
        RqstInts rqstInts = new RqstInts();
        rqstInts.value = Arrays.stream(new int[]{respBarracks.buildingRix, SpeedTimeType.TRAIN.getType()}).boxed().collect(Collectors.toList());
        Map<String, RqstInts> is = rqstUseItem.params.is = new HashMap<>();
        is.put("chgTimeParam", rqstInts);
        return connect.send(Commands.ITEM_USE_CONST, rqstUseItem);
    }
}
