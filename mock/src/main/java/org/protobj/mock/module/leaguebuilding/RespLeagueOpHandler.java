package org.protobj.mock.module.leaguebuilding;

import com.guangyu.cd003.projects.gs.module.leaguebuilding.msg.RespLeagueOp;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespLeagueOpHandler implements RespHandler<RespLeagueOp> {

	@Override
	public void handle(MockConnect connect, RespLeagueOp respMsg, int cmd) {
		connect.LAST_RECV_MSGS.put(subCmd(), respMsg);

	}

	@Override
	public int subCmd() {
		return 2201;
	}
}