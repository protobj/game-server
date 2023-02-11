package org.protobj.mock.module.guardtower;

import com.guangyu.cd003.projects.gs.module.guardtower.msg.RespGuardTowerInfo;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespGuardTowerInfoHandler implements RespHandler<RespGuardTowerInfo> {

	@Override
	public void handle(MockConnect connect, RespGuardTowerInfo respMsg, int cmd) {
		//connect.LAST_RECV_MSGS.put(subCmd(), respMsg);

	}

	@Override
	public int subCmd() {
		return 3301;
	}
}
