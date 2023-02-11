package org.protobj.mock.module.roleInfostat;

import com.guangyu.cd003.projects.gs.module.roleInfostat.msg.RespRoleStatOP;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespRoleStatOPHandler implements RespHandler<RespRoleStatOP> {

	@Override
	public void handle(MockConnect connect, RespRoleStatOP respMsg, int cmd) {
		//connect.LAST_RECV_MSGS.put(subCmd(), respMsg);

	}

	@Override
	public int subCmd() {
		return 4602;
	}
}
