package org.protobj.mock.module.role;

import com.guangyu.cd003.projects.gs.module.role.msg.RespRoleOp;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespRoleOpHandler implements RespHandler<RespRoleOp> {

	@Override
	public void handle(MockConnect connect, RespRoleOp respMsg, int cmd) {
		//connect.LAST_RECV_MSGS.put(subCmd(), respMsg);

	}

	@Override
	public int subCmd() {
		return 102;
	}
}
