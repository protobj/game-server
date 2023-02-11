package org.protobj.mock.module.city;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import com.guangyu.cd003.projects.gs.module.city.msg.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import com.guangyu.cd003.projects.gs.module.city.cfg.BuildingCdit;
import com.guangyu.cd003.projects.gs.module.city.cfg.BuildingCfg;
import com.guangyu.cd003.projects.gs.module.city.cfg.CityCfg;
import com.guangyu.cd003.projects.gs.module.city.cfg.DscrDataCity;
import com.guangyu.cd003.projects.gs.module.city.cons.ConstCity;
import com.guangyu.cd003.projects.gs.module.common.vo.PosShort;
import com.guangyu.cd003.projects.gs.module.common.vo.PosShortObj;
import com.guangyu.cd003.projects.gs.module.legion.cfg.LegionUnitCfg;
import com.pv.framework.gs.core.cfg.core.CacheConfig;

import io.netty.util.collection.IntObjectMap;

public class CityData {
    public static List<Integer> buildingIds = Arrays.stream(new int[]{102000001, 102000002, 102000004, 102000005, 102000006, 102000007, 102000009, 102000010, 102000011, 102000013, 102000014, 102000015, 102000016, 102000017, 102000018, 102000019, 102000020, 102000021, 102000022, 102000023, 102000024, 102000025, 102000026}).boxed().collect(Collectors.toList());
    public RespCityInfo respCityInfo;

    // type->maxLv
    public Map<Integer, Integer> maxBuildingLv = new HashMap<>();
    // type->count
    public Map<Integer, Integer> buildCount = new HashMap<>();

    //可升级的建筑
    public List<Integer> canUpgradeBuildingRix = new ArrayList<>();

    //可建造的建筑
    public List<Integer> canCreBuildingCids = new ArrayList<>();


    private BitSet pit = new BitSet(ConstCity.PIT_SCOPE_MAX_WIDTH * ConstCity.PIT_SCOPE_MAX_WIDTH);//基坑 目前60*60

    public AddBuildingState addBuildingState;

    private CityCfg cfg;

    public Map<Integer, Integer> getAvailableCorps() {
        Map<Integer, Integer> corps = respCityInfo.corps;
        Map<Integer, Integer> integerIntegerHashMap = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : corps.entrySet()) {
            LegionUnitCfg cfgByClz = CacheConfig.getCfg(LegionUnitCfg.class, entry.getKey());
            if (cfgByClz.getSpd() > 100 && entry.getValue() > 1000) {
                integerIntegerHashMap.put(entry.getKey(), entry.getValue());
            }
        }
        return integerIntegerHashMap;
    }

    public void handle(RespCityInfo respCityInfo) {
        this.respCityInfo = respCityInfo;
        cfg = getCfg();
        updateBuilding(respCityInfo);
        boolean freeWorker = findFreeWorker();
        if (freeWorker) {
            setAddBuildingState(AddBuildingState.IDLE);
        } else {
            setAddBuildingState(AddBuildingState.RQST_CRE_WORKER);
        }
        printAtkLv(respCityInfo.canAtkNpcLvMap);

    }

    private void printAtkLv(Map<Integer, RespCanAtkNpcLv> canAtkNpcLvMap) {
//        canAtkNpcLvMap.forEach((k,v)->{
//            System.err.println(k + ":" + v.npcTypeLv);
//        });
//        Thread.dumpStack();
    }

    private void updateBuilding(RespCityInfo respCityInfo) {
        maxBuildingLv.clear();
        buildCount.clear();
        pit.clear(0, pit.size());
        for (RespBuilding building : respCityInfo.buildings) {
            updBuildingMaxLv(building);
            updBuildingCount(building);
            resetBuildingPit(building, true);
        }
    }

    private void resetBuildingPit(RespBuilding building, boolean set) {
        int maxX = cfg.getMaxPitX();
        int minX = cfg.getMinPitX();
        int maxZ = cfg.getMaxPitZ();
        int minZ = cfg.getMinPitZ();
        int x = building.x;
        int z = building.z;
        BuildingCfg buildingCfg = CacheConfig.getCfg(BuildingCfg.class, building.cid);
        int w = buildingCfg.getReqPitW();
        int l = buildingCfg.getReqPitL();
        if (w <= 0 || l <= 0) return;
        if (x < minX || x > maxX || z < minZ || z > maxZ || x + w - 1 > maxX || z + l - 1 > maxZ) return;
        int maxW = ConstCity.PIT_SCOPE_MAX_WIDTH;
        maxZ = Math.min(maxZ, z + l - 1);
        for (int i = z; i <= maxZ; i++) {
            int start = x + i * maxW;
            pit.set(start, start + w - 1, set);
        }
    }

    private CityCfg getCfg() {
        IntObjectMap<CityCfg> cfgsByClz = CacheConfig.getCfgsByClz(CityCfg.class);
        for (CityCfg value : cfgsByClz.values()) {
            if (value.getLv() == respCityInfo.lv) {
                return value;
            }
        }
        return null;
    }

    public PosShort findFreePit(BuildingCfg buildingCfg) {

        int w = buildingCfg.getReqPitW();
        int l = buildingCfg.getReqPitL();
        if (w <= 0 || l <= 0) {
            return PosShortObj.of(0, 0);
        }
        for (int i = 0; i < ConstCity.PIT_SCOPE_MAX_WIDTH; i++) {
            for (int j = 0; j < ConstCity.PIT_SCOPE_MAX_WIDTH; j++) {
                if (!intersect(i, j, w, l)) {
                    return PosShortObj.of(i, j);
                }
            }
        }
        return null;
    }

    private boolean intersect(int x, int z, int w, int l) {
        int maxX = cfg.getMaxPitX();
        int minX = cfg.getMinPitX();
        int maxZ = cfg.getMaxPitZ();
        int minZ = cfg.getMinPitZ();
        if (w <= 0 || l <= 0)
            return false;
        if (x < minX || x > maxX || z < minZ || z > maxZ || x + w - 1 > maxX || z + l - 1 > maxZ)
            return true;
        int maxW = ConstCity.PIT_SCOPE_MAX_WIDTH;
        maxZ = Math.min(maxZ, z + l - 1);
        for (int i = z; i <= maxZ; i++) {
            int start = x + i * maxW;
            int end = start + w;
            for (int j = start; j < end; j++) {
                if (pit.get(j))
                    return true;
            }
        }
        return false;
    }

    //可增加的建筑
    public void resetCanCreBuildings() {
        canCreBuildingCids.clear();
        for (Integer buildId : buildingIds) {
            BuildingCfg buildingCfg = CacheConfig.getCfg(BuildingCfg.class, buildId);
            byte maxNum = buildingCfg.getMaxNum(respCityInfo.lv);
            Integer curNum = buildCount.getOrDefault((int) buildingCfg.getType(), 0);
            if (curNum >= maxNum) {
                continue;
            }
            canCreBuildingCids.add(buildId);
        }
    }
    //可升级的建筑

    public void resetCanUpgradeBuilding() {
        canUpgradeBuildingRix.clear();
        for (RespBuilding building : respCityInfo.buildings) {
            int cid = building.cid;
            BuildingCfg cfg = CacheConfig.getCfg(BuildingCfg.class, cid);
            if (building.lv >= cfg.getMaxLv()) {
                continue;
            }
            Collection<BuildingCdit> buildingCdits = getCdits(cfg, building.lv + 1);
            if (!buildingCdits.isEmpty()) {
                boolean anyMatch = buildingCdits.stream().anyMatch(it -> !validateBuildingLv(it.getType(), it.getReqLv()));
                if (anyMatch) {
                    continue;
                }
            }
            canUpgradeBuildingRix.add(building.rix);
        }
    }

    public boolean validateBuildingLv(int buildingType, int lv) {
        return maxBuildingLv.getOrDefault(buildingType, 0) >= lv;
    }

    public static Collection<BuildingCdit> getCdits(BuildingCfg cfg, int lv) {
        List<BuildingCdit>[] cditsAry = cfg.getBuildingCdits();
        //等级-1作为配置下标
        lv = Math.min(cditsAry.length - 1, lv);
        if (lv < 0) return Collections.emptyList();
        return cditsAry[lv];
    }

    private void updBuildingCount(RespBuilding building) {
        buildCount.merge((int) CacheConfig.getCfg(BuildingCfg.class, building.cid).getType(), 1, Integer::sum);
    }

    public void updBuildingMaxLv(RespBuilding building) {
        maxBuildingLv.compute((int) CacheConfig.getCfg(BuildingCfg.class, building.cid).getType(), (type, lv) -> {
            int curLv = building.lv;
            if (lv == null) return curLv;
            return Math.max(lv, curLv);
        });
    }


    public void handle(RespCityOp respCityOp) {
        if (this.respCityInfo == null) {
            return;
        }
        if (respCityOp.chgX != null) {
            respCityInfo.x = respCityOp.chgX.value;
        }
        if (respCityOp.chgZ != null) {
            respCityInfo.z = respCityOp.chgZ.value;
        }
        if (respCityOp.chgLv != null) {
            respCityInfo.lv = respCityOp.chgLv.value;
            cfg = getCfg();
        }
        if (respCityOp.chgCamp != null) {
            respCityInfo.camp = respCityOp.chgCamp.value;
        }
        if (CollectionUtils.isNotEmpty(respCityOp.delBuildings)) {
            boolean removeIf = respCityInfo.buildings.removeIf(it -> respCityOp.delBuildings.contains(it.rix));
            if (removeIf) {
                updateBuilding(respCityInfo);
            }
        }
        if (CollectionUtils.isNotEmpty(respCityOp.chgBuildings)) {
            respCityInfo.buildings.removeIf(it -> respCityOp.chgBuildings.stream().anyMatch(t -> t.rix == it.rix));
            respCityInfo.buildings.addAll(respCityOp.chgBuildings);
            updateBuilding(respCityInfo);
        }
        if (respCityOp.updRsrcs != null) {
            respCityInfo.resources.putAll(respCityOp.updRsrcs);
        }
        if (respCityOp.cityShield != null) {
            respCityInfo.cityShield = respCityOp.cityShield.value;
        }
        if (respCityOp.additionTrainCap != null) {
            respCityInfo.additionTrainCap = respCityOp.additionTrainCap.value;
        }
        if (respCityOp.warState != null) {
            respCityInfo.warState = respCityOp.warState.value;
        }
        if (CollectionUtils.isNotEmpty(respCityOp.workers)) {
            respCityInfo.workers.removeIf(it -> respCityOp.workers.stream().anyMatch(t -> t.rix.value == it.rix.value));
            respCityInfo.workers.addAll(respCityOp.workers);
            boolean freeWorker = findFreeWorker();
            if (freeWorker) {
                setAddBuildingState(AddBuildingState.IDLE);
            }
        }
        if(MapUtils.isNotEmpty(respCityOp.seriWndedUpds)) {
        	respCityInfo.seriWndeds =respCityOp.seriWndedUpds;
        }

        if (MapUtils.isNotEmpty(respCityOp.corpsUpds)) {
            respCityInfo.corps.putAll(respCityOp.corpsUpds);
        }
        if (MapUtils.isNotEmpty(respCityOp.trainedCorpsUpds)) {
            respCityInfo.trainedCorps.putAll(respCityOp.trainedCorpsUpds);
        }
        if (MapUtils.isNotEmpty(respCityOp.canAtkNpcLvMap)) {
            respCityInfo.canAtkNpcLvMap.putAll(respCityOp.canAtkNpcLvMap);
            printAtkLv(respCityInfo.canAtkNpcLvMap);
        }
    }

    public boolean findFreeWorker() {
        long now = System.currentTimeMillis() / 1000;
        for (RespCityWorker worker : respCityInfo.workers) {
            if (worker.buildingRix.value < 0 && (worker.endTime.value == -1 || worker.endTime.value > now)) {
                return true;
            }

        }
        return false;
    }

    public int needPayWorkerCount() {
        int count = 0;
        long now = System.currentTimeMillis() / 1000;
        for (RespCityWorker worker : respCityInfo.workers) {
            if (worker.endTime.value == -1) {//免费队列
                continue;
            }
            if (worker.endTime.value < now) {
                count++;
            }
        }
        return count;
    }

    public void setAddBuildingState(AddBuildingState addBuildingState) {
        this.addBuildingState = addBuildingState;
    }

    public void handle(RespProfitCity respProfit) {
        respProfit.addRsrcs.forEach((k, v) -> {
            this.respCityInfo.resources.merge(k, v, new BiFunction<Long, Long, Long>() {
                @Override
                public Long apply(Long aLong, Long aLong2) {
                    if (aLong == null) {
                        aLong = 0L;
                    }
                    if (aLong2 == null) {
                        aLong2 = 0L;
                    }
                    return aLong + aLong2;
                }
            });
        });
    }

    public int getBaseLegionLeaderShip() {
        int[] data = DscrDataCity.LEGION_NUM_MIN_LIMIT.getData();
        int dataIx = Math.min(respCityInfo.lv, data.length - 1);
        return data[dataIx];
    }
}
