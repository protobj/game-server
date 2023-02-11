package org.protobj.mock.module.leagueextra;

import com.guangyu.cd003.projects.common.module.leagueextra.msg.RespLeagueLogInfo;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespLeagueLogInfoHandler implements RespHandler<RespLeagueLogInfo> {

	@Override
	public void handle(MockConnect connect, RespLeagueLogInfo respMsg, int cmd) {
		//connect.LAST_RECV_MSGS.put(subCmd(), respMsg);

	}

	@Override
	public int subCmd() {
		return 2801;
	}
}
