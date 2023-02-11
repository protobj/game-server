package org.protobj.mock.module.city;

import com.guangyu.cd003.projects.gs.module.city.msg.RespCityOp;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespCityOpHandler implements RespHandler<RespCityOp> {

	@Override
	public void handle(MockConnect connect, RespCityOp respMsg, int cmd) {
		//connect.LAST_RECV_MSGS.put(subCmd(), respMsg);
		connect.CITY_DATA.handle(respMsg);
	}

	@Override
	public int subCmd() {
		return 202;
	}
}
