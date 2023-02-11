package org.protobj.mock.module.reddot;

import com.guangyu.cd003.projects.common.module.reddot.msg.RespRedDotOp;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespRedDotOpHandler implements RespHandler<RespRedDotOp> {

	@Override
	public void handle(MockConnect connect, RespRedDotOp respMsg, int cmd) {
		connect.LAST_RECV_MSGS.put(subCmd(), respMsg);

	}

	@Override
	public int subCmd() {
		return 5302;
	}
}
