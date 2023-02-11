package org.protobj.mock.module.depot;

import com.guangyu.cd003.projects.gs.module.depot.msg.RespDepotInfo;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespDepotInfoHandler implements RespHandler<RespDepotInfo> {

	@Override
	public void handle(MockConnect connect, RespDepotInfo respMsg, int cmd) {
		//connect.LAST_RECV_MSGS.put(subCmd(), respMsg);
		connect.DEPOT_DATA.handle(respMsg);
	}

	@Override
	public int subCmd() {
		return 801;
	}
}
