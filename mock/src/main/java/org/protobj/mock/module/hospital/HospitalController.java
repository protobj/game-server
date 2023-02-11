package org.protobj.mock.module.hospital;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.collections.MapUtils;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.guangyu.cd003.projects.common.cons.Commands;
import com.guangyu.cd003.projects.gs.module.city.cons.SyncFuncTypeCity;
import com.guangyu.cd003.projects.gs.module.city.msg.RespCityInfo;
import com.guangyu.cd003.projects.gs.module.hospital.cache.CacheHospital;
import com.guangyu.cd003.projects.gs.module.hospital.cfg.CureCorpsInfoCfg;
import com.guangyu.cd003.projects.gs.module.hospital.msg.RespHospitalInfo;
import com.guangyu.cd003.projects.gs.module.hospital.msg.RqstHospitalCure;
import com.guangyu.cd003.projects.gs.module.legion.cfg.LegionUnitCfg;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.pv.common.utilities.common.CommonUtil;
import com.pv.framework.gs.core.cfg.core.CacheConfig;
import com.pv.framework.gs.core.module.sys.vo.FuncConfig;
import com.pv.framework.gs.core.util.RandomUtils;

public class HospitalController {

    public static void cure(MockConnect connect) {
    	HospitalData hospitalData = connect.HOSPITAL_DATA;
    	try {
            CompletableFuture<Integer> future = proc(connect);
            future.exceptionally(throwable -> {
                hospitalData.resetState();
                return null;
            });
        } catch (Exception e) {
        	 hospitalData.resetState();
        }
    }
    
    
    private static CompletableFuture<Integer> proc(MockConnect connect) {
    	HospitalData hospitalData = connect.HOSPITAL_DATA;
    	RespHospitalInfo respHospitalInfo = hospitalData.respHospitalInfo;
    	HospitalState hospState = null;
    	Set<Integer> keySet = respHospitalInfo.hospital.keySet();
    	for (Integer rix : keySet) {
    		hospState = connect.HOSPITAL_DATA.getHospitalState(rix.intValue());
    		break;
		}
    	int hospStateCode = hospState.getCurState();
    	if(hospStateCode == HospitalState.IDLE) {
    		hospitalData.stateChange(HospitalState.RQST_CURE);
    		return cure(connect, hospitalData);
    	} else if (hospStateCode == HospitalState.HAVE_NO_RSRC) {
    		return cure(connect, hospitalData);
        }else if(hospStateCode == HospitalState.CURE_COMPLETE) {
        	hospitalData.stateChange(HospitalState.RQST_COLLECT);
        	return connect.send(Commands.HOSPITAL_COLLECT_CONST, null);
        }else if(hospStateCode == HospitalState.CURE) {
        	if(RandomUtils.nextInt(10000) < 1000) {
//        		connect.sendChatCMD("addrsrc", 2,10000000);
        		hospitalData.stateChange(HospitalState.RQST_IMMEDIATE);
        		return connect.send(Commands.HOSPITAL_TRAIN_IMMEDIATE_CONST, null);
        	}
        }
    	return CompletableFuture.completedFuture(0);
    }
    
    private static CompletableFuture<Integer> cure(MockConnect connect,HospitalData hospitalData){
    	Map<Integer, Integer> seriWndeds = gocSeriWndeds(connect);
    	if(MapUtils.isEmpty(seriWndeds)) {
    		hospitalData.stateChange(HospitalState.IDLE);
//    		if(RandomUtils.nextInt(10000) < 1000) {
//    			return connect.sendChatCMD("chgSeriWndeds", 113000001,1000);
//    		}
    		return CompletableFuture.completedFuture(0);
    	}
    	//判断资源是否足够
        boolean rsrcEnough = checkRsrcEnough(connect,clacRsrc(seriWndeds));
        if (!rsrcEnough) {
            hospitalData.stateChange(HospitalState.HAVE_NO_RSRC);
            return CompletableFuture.completedFuture(0);
        }
    	RqstHospitalCure rqstCureMsg = new RqstHospitalCure();
    	rqstCureMsg.cureMap = new HashMap<Integer, Integer>(seriWndeds);
    	rqstCureMsg.immediate = RandomUtils.nextInt(10000) < 1000;
    	 int cmd = Commands.HOSPITAL_TRAIN_CONST;
         return connect.send(cmd, rqstCureMsg);
    }
    
    
    
    private static Map<Integer, Integer> gocSeriWndeds(MockConnect connect){
    	RespCityInfo cityInfo = connect.CITY_DATA.respCityInfo;
    	Map<Integer, Integer> seriWndeds = CommonUtil.createSimpleMap();
    	if(MapUtils.isNotEmpty(cityInfo.seriWndeds)) {
    		cityInfo.seriWndeds.forEach((id,num)->{
    			if(num > 0) {
    				seriWndeds.put(id, num);
    			}
    		});
    	}
    	return seriWndeds;
    }
    
    private static boolean checkRsrcEnough(MockConnect connect, List<FuncConfig> rsrcCostsTypeCounts) {
        for (FuncConfig rsrcCostsTypeCount : rsrcCostsTypeCounts) {
            if (SyncFuncTypeCity.CHG_RSRC_NUM.getType() == rsrcCostsTypeCount.getType()) {
                int param = rsrcCostsTypeCount.getParam();
                Long orDefault = connect.CITY_DATA.respCityInfo.resources.getOrDefault(param, 0L);
                if (rsrcCostsTypeCount.getValue() > orDefault) {
//                	connect.sendChatCMD("addrsrc", param,10000000);
                    return false;
                }
            }
        }
        return true;
    }
    
    private static List<FuncConfig> clacRsrc(Map<Integer, Integer> cures){
    	 HashBasedTable<Integer, Integer, Integer> cost = HashBasedTable.create();
         for (Map.Entry<Integer,Integer> e : cures.entrySet()) {
             Integer unitCid = e.getKey();
             int cureNum = e.getValue().intValue();
             
             LegionUnitCfg unitCfg = CacheConfig.getCfg(LegionUnitCfg.class, unitCid);//CacheLegion.getLegionUnitCfgBy(unitCid);
             if(unitCfg == null) {
            	 System.out.println("not find unitCid :"+unitCid);
            	 continue;
             }
             CureCorpsInfoCfg cureCorpsInfoCfg = CacheHospital.getCureCorpsInfoCfg(unitCid);
             Set<Table.Cell<Integer, Integer, Integer>> cureCosts = cureCorpsInfoCfg.getCureRsrcCosts().cellSet();
             for (Table.Cell<Integer, Integer, Integer> cureCost : cureCosts) {
                 Integer type = cureCost.getRowKey();
                 Integer param = cureCost.getColumnKey();
                 int costNum = cureCost.getValue();
                 Integer curCostNum = cost.get(type, param);
                 if (curCostNum == null) {
                     cost.put(type, param, costNum * cureNum);
                 } else {
                     cost.put(type, param, costNum * cureNum + curCostNum.intValue());
                 }
             }
         }
         List<FuncConfig> funcConfigs = new ArrayList<>();
         for (Table.Cell<Integer, Integer, Integer> cell : cost.cellSet()) {
             funcConfigs.add(new FuncConfig(cell.getRowKey(), cell.getColumnKey(), Math.abs(cell.getValue())));
         }
         return funcConfigs;
    }
}
