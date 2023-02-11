package org.protobj.mock.module.league;

import com.guangyu.cd003.projects.gs.module.league.msg.RespLeagueCacheOp;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespLeagueCacheOpHandler implements RespHandler<RespLeagueCacheOp> {

	@Override
	public void handle(MockConnect connect, RespLeagueCacheOp respMsg, int cmd) {
		connect.LAST_RECV_MSGS.put(subCmd(), respMsg);

	}

	@Override
	public int subCmd() {
		return 2205;
	}
}
