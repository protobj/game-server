package org.protobj.mock.module.smithy;

import com.guangyu.cd003.projects.gs.module.smithy.msg.RespSmithyInfo;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespSmithyInfoHandler implements RespHandler<RespSmithyInfo> {

	@Override
	public void handle(MockConnect connect, RespSmithyInfo respMsg, int cmd) {
		//connect.LAST_RECV_MSGS.put(subCmd(), respMsg);

	}

	@Override
	public int subCmd() {
		return 3401;
	}
}
