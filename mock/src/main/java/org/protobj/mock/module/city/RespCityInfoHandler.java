package org.protobj.mock.module.city;

import com.guangyu.cd003.projects.gs.module.city.cons.RespTypeCity;
import com.guangyu.cd003.projects.gs.module.city.msg.RespCityInfo;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespCityInfoHandler implements RespHandler<RespCityInfo> {

	@Override
	public void handle(MockConnect connect, RespCityInfo respMsg, int cmd) {
		//connect.LAST_RECV_MSGS.put(subCmd(), respMsg);
		connect.CITY_DATA.handle(respMsg);
	}

	@Override
	public int subCmd() {
		return RespTypeCity.RESP_TYPE_CITY_INFO;
	}
}
