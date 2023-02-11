package org.protobj.mock.module.cbt;

import com.guangyu.cd003.projects.gs.module.cbt.msg.RespCbtOp;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespCbtOpHandler implements RespHandler<RespCbtOp> {

	@Override
	public void handle(MockConnect connect, RespCbtOp respMsg, int cmd) {
		//connect.LAST_RECV_MSGS.put(subCmd(), respMsg);

	}

	@Override
	public int subCmd() {
		return 510;
	}
}
