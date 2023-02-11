package org.protobj.mock.module.reconnoitre;

import com.guangyu.cd003.projects.gs.module.reconnoitre.msg.RespReconnonitreInfo;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespReconnonitreInfoHandler implements RespHandler<RespReconnonitreInfo> {

	@Override
	public void handle(MockConnect connect, RespReconnonitreInfo respMsg, int cmd) {
		//connect.LAST_RECV_MSGS.put(subCmd(), respMsg);

	}

	@Override
	public int subCmd() {
		return 1901;
	}
}
