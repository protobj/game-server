package org.protobj.mock.module.uav;

import com.guangyu.cd003.projects.gs.module.uav.msg.RespUavInfo;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespUavInfoHandler implements RespHandler<RespUavInfo> {

	@Override
	public void handle(MockConnect connect, RespUavInfo respMsg, int cmd) {
		//connect.LAST_RECV_MSGS.put(subCmd(), respMsg);

	}

	@Override
	public int subCmd() {
		return 4801;
	}
}
