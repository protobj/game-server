package org.protobj.mock.module.leagueextra;

import com.guangyu.cd003.projects.common.module.leagueextra.msg.RespLeagueRankInfo;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespLeagueRankInfoHandler implements RespHandler<RespLeagueRankInfo> {

	@Override
	public void handle(MockConnect connect, RespLeagueRankInfo respMsg, int cmd) {
		//connect.LAST_RECV_MSGS.put(subCmd(), respMsg);

	}

	@Override
	public int subCmd() {
		return 2802;
	}
}
