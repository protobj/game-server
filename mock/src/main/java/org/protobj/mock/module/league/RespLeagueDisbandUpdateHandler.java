package org.protobj.mock.module.league;

import com.guangyu.cd003.projects.common.module.league.msg.RespLeagueDisbandUpdate;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespLeagueDisbandUpdateHandler implements RespHandler<RespLeagueDisbandUpdate> {

	@Override
	public void handle(MockConnect connect, RespLeagueDisbandUpdate respMsg, int cmd) {
		//connect.LAST_RECV_MSGS.put(subCmd(), respMsg);
		connect.LEAGUE_DATA.handle(connect,respMsg);
	}

	@Override
	public int subCmd() {
		return 2202;
	}
}
