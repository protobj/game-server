package org.protobj.mock.module.rank;

import com.guangyu.cd003.projects.gs.module.rank.msg.RespRankInfo;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespRankInfoHandler implements RespHandler<RespRankInfo> {

	@Override
	public void handle(MockConnect connect, RespRankInfo respMsg, int cmd) {
		//connect.LAST_RECV_MSGS.put(subCmd(), respMsg);

	}

	@Override
	public int subCmd() {
		return 9701;
	}
}
