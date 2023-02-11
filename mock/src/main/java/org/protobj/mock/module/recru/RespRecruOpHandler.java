package org.protobj.mock.module.recru;

import com.guangyu.cd003.projects.gs.module.recru.msg.RespRecruOp;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespRecruOpHandler implements RespHandler<RespRecruOp> {

	@Override
	public void handle(MockConnect connect, RespRecruOp respMsg, int cmd) {
		//connect.LAST_RECV_MSGS.put(subCmd(), respMsg);

	}

	@Override
	public int subCmd() {
		return 1502;
	}
}
