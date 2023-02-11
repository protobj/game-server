package org.protobj.mock.module.barracks;

import com.guangyu.cd003.projects.gs.module.barracks.msg.RespBarracksOp;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespBarracksOpHandler implements RespHandler<RespBarracksOp> {

	@Override
	public void handle(MockConnect connect, RespBarracksOp respMsg, int cmd) {
		//connect.LAST_RECV_MSGS.put(subCmd(), respMsg);
		connect.BARRACKS_DATA.handle(respMsg);
	}

	@Override
	public int subCmd() {
		return 1302;
	}
}
