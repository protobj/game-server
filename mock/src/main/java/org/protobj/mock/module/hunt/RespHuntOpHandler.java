package org.protobj.mock.module.hunt;

import com.guangyu.cd003.projects.common.module.hunt.msg.RespHuntOp;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespHuntOpHandler implements RespHandler<RespHuntOp> {

	@Override
	public void handle(MockConnect connect, RespHuntOp respMsg, int cmd) {
		//connect.LAST_RECV_MSGS.put(subCmd(), respMsg);

	}

	@Override
	public int subCmd() {
		return 3501;
	}
}
