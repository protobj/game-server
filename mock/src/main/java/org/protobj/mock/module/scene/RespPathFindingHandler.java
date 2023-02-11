package org.protobj.mock.module.scene;

import com.guangyu.cd003.projects.gs.module.scene.msg.RespPathFinding;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespPathFindingHandler implements RespHandler<RespPathFinding> {

	@Override
	public void handle(MockConnect connect, RespPathFinding respMsg, int cmd) {
		//connect.LAST_RECV_MSGS.put(subCmd(), respMsg);

	}

	@Override
	public int subCmd() {
		return 303;
	}
}
