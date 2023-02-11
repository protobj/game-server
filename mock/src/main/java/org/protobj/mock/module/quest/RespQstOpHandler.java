package org.protobj.mock.module.quest;

import com.guangyu.cd003.projects.gs.module.quest.msg.RespQstOp;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespQstOpHandler implements RespHandler<RespQstOp> {

	@Override
	public void handle(MockConnect connect, RespQstOp respMsg, int cmd) {
		//connect.LAST_RECV_MSGS.put(subCmd(), respMsg);

	}

	@Override
	public int subCmd() {
		return 4502;
	}
}
