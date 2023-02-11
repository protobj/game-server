package org.protobj.mock.module.scientific;

import com.guangyu.cd003.projects.gs.module.scientific.msg.RespScientificOP;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespScientificOPHandler implements RespHandler<RespScientificOP> {

	@Override
	public void handle(MockConnect connect, RespScientificOP respMsg, int cmd) {
		//connect.LAST_RECV_MSGS.put(subCmd(), respMsg);

	}

	@Override
	public int subCmd() {
		return 2302;
	}
}
