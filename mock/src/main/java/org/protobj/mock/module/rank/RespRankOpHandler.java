package org.protobj.mock.module.rank;

import com.guangyu.cd003.projects.gs.module.rank.msg.RespRankOp;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespRankOpHandler implements RespHandler<RespRankOp> {

	@Override
	public void handle(MockConnect connect, RespRankOp respMsg, int cmd) {
		//connect.LAST_RECV_MSGS.put(subCmd(), respMsg);

	}

	@Override
	public int subCmd() {
		return 9702;
	}
}
