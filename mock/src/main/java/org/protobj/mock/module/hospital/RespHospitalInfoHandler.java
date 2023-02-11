package org.protobj.mock.module.hospital;

import com.guangyu.cd003.projects.gs.module.hospital.msg.RespHospitalInfo;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespHospitalInfoHandler implements RespHandler<RespHospitalInfo> {

	@Override
	public void handle(MockConnect connect, RespHospitalInfo respMsg, int cmd) {
		//connect.LAST_RECV_MSGS.put(subCmd(), respMsg);
		connect.HOSPITAL_DATA.handle(respMsg);
	}

	@Override
	public int subCmd() {
		return 1401;
	}
}
