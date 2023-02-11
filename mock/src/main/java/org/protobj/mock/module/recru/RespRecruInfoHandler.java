package org.protobj.mock.module.recru;

import com.guangyu.cd003.projects.gs.module.recru.msg.RespRecruInfo;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespRecruInfoHandler implements RespHandler<RespRecruInfo> {

	@Override
	public void handle(MockConnect connect, RespRecruInfo respMsg, int cmd) {
		//connect.LAST_RECV_MSGS.put(subCmd(), respMsg);

	}

	@Override
	public int subCmd() {
		return 1501;
	}
}
