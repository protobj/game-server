package org.protobj.mock.module.role;

import com.guangyu.cd003.projects.gs.module.role.msg.RespRoleInfo;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespRoleInfoHandler implements RespHandler<RespRoleInfo> {

	@Override
	public void handle(MockConnect connect, RespRoleInfo respMsg, int cmd) {
		//connect.LAST_RECV_MSGS.put(subCmd(), respMsg);
		connect.ROLE_DATA.handle(respMsg);
	}

	@Override
	public int subCmd() {
		return 101;
	}
}
