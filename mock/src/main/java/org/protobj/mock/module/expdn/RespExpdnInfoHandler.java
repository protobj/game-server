package org.protobj.mock.module.expdn;

import com.guangyu.cd003.projects.gs.module.expdn.msg.RespExpdnInfo;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespExpdnInfoHandler implements RespHandler<RespExpdnInfo> {

	@Override
	public void handle(MockConnect connect, RespExpdnInfo respMsg, int cmd) {
		//connect.LAST_RECV_MSGS.put(subCmd(), respMsg);

	}

	@Override
	public int subCmd() {
		return 4201;
	}
}
