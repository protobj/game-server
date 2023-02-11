package org.protobj.mock.module.guide;

import com.guangyu.cd003.projects.gs.module.guide.msg.RespGuideInfo;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespGuideInfoHandler implements RespHandler<RespGuideInfo> {

	@Override
	public void handle(MockConnect connect, RespGuideInfo respMsg, int cmd) {
		//connect.LAST_RECV_MSGS.put(subCmd(), respMsg);

	}

	@Override
	public int subCmd() {
		return 4701;
	}
}
