package org.protobj.mock.module.uav;

import com.guangyu.cd003.projects.gs.module.uav.msg.RespUavOp;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespUavOpHandler implements RespHandler<RespUavOp> {

	@Override
	public void handle(MockConnect connect, RespUavOp respMsg, int cmd) {
		//connect.LAST_RECV_MSGS.put(subCmd(), respMsg);

	}

	@Override
	public int subCmd() {
		return 4802;
	}
}
