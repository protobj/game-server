package org.protobj.mock.module.hospital;

import java.util.HashMap;
import java.util.Map;

import com.guangyu.cd003.projects.gs.module.common.msg.RespTimeQuantify;
import org.apache.commons.collections.MapUtils;

import com.guangyu.cd003.projects.gs.module.hospital.msg.RespHospital;
import com.guangyu.cd003.projects.gs.module.hospital.msg.RespHospitalInfo;
import com.guangyu.cd003.projects.gs.module.hospital.msg.RespHospitalOp;

public class HospitalData {

    RespHospitalInfo respHospitalInfo;

    Map<Integer, HospitalState> stateMap = new HashMap<>();
    
    public void handle(RespHospitalInfo respHospitalInfo) {
        this.respHospitalInfo = respHospitalInfo;
        resetState();
    }

    public void handle(RespHospitalOp respHospitalOp) {
        if (respHospitalOp.hospitalUpd != null) {
            Map<Integer, RespHospital> hospital = respHospitalInfo.hospital;
            if (hospital != null) {
                hospital.putAll(respHospitalOp.hospitalUpd);
            }else{
                respHospitalInfo.hospital = respHospitalOp.hospitalUpd;
            }
        }
        if (MapUtils.isNotEmpty(respHospitalOp.cureMapUpd)) {
            respHospitalInfo.cureMap = respHospitalOp.cureMapUpd;
        }
        if (respHospitalOp.cureEndTimeUpd != null) {
            respHospitalInfo.cureEndTime = respHospitalOp.cureEndTimeUpd;
        }
        resetState();
    }
    
    public void stateChange(int state) {
    	respHospitalInfo.hospital.forEach((rix,hospotal)->{
    		HospitalState hospotalState = getHospitalState(rix);
    		hospotalState.setCurState(state);
    	});
    }
    
    public HospitalState getHospitalState(int rix) {
    	return stateMap.computeIfAbsent(rix, v->new HospitalState());
    }
    
    public void resetState() {
        int curSec = (int) (System.currentTimeMillis() / 1000);
        RespTimeQuantify cureEndTime = respHospitalInfo.cureEndTime;
        if(cureEndTime.endSec <= 0) {
        	stateChange(HospitalState.IDLE);
        }else if (cureEndTime.endSec > curSec) {
        	stateChange(HospitalState.CURE);
        } else if(cureEndTime.endSec <= curSec){
        	stateChange(HospitalState.CURE_COMPLETE);
        }
    }
}
