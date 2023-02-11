package org.protobj.mock.module.scientific;

import com.guangyu.cd003.projects.gs.module.scientific.msg.RespScientificInfo;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespScientificInfoHandler implements RespHandler<RespScientificInfo> {

	@Override
	public void handle(MockConnect connect, RespScientificInfo respMsg, int cmd) {
		//connect.LAST_RECV_MSGS.put(subCmd(), respMsg);

	}

	@Override
	public int subCmd() {
		return 2301;
	}
}
