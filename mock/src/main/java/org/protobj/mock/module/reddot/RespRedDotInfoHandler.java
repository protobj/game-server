package org.protobj.mock.module.reddot;

import com.guangyu.cd003.projects.common.module.reddot.msg.RespRedDotInfo;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespRedDotInfoHandler implements RespHandler<RespRedDotInfo> {

	@Override
	public void handle(MockConnect connect, RespRedDotInfo respMsg, int cmd) {
		connect.LAST_RECV_MSGS.put(subCmd(), respMsg);

	}

	@Override
	public int subCmd() {
		return 5301;
	}
}
