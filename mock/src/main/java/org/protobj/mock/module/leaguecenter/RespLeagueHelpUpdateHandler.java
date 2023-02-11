package org.protobj.mock.module.leaguecenter;

import com.guangyu.cd003.projects.common.module.leaguecenter.msg.RespLeagueHelpUpdate;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespLeagueHelpUpdateHandler implements RespHandler<RespLeagueHelpUpdate> {

	@Override
	public void handle(MockConnect connect, RespLeagueHelpUpdate respMsg, int cmd) {
		//connect.LAST_RECV_MSGS.put(subCmd(), respMsg);

	}

	@Override
	public int subCmd() {
		return 2702;
	}
}
