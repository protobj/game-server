package org.protobj.mock.module.leagueSeizeble;

import com.guangyu.cd003.projects.gs.module.leagueSeizeble.msg.RespLeagueSeizebleOp;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespLeagueSeizebleOpHandler implements RespHandler<RespLeagueSeizebleOp> {

	@Override
	public void handle(MockConnect connect, RespLeagueSeizebleOp respMsg, int cmd) {
		//connect.LAST_RECV_MSGS.put(subCmd(), respMsg);

	}

	@Override
	public int subCmd() {
		return 4103;
	}
}
