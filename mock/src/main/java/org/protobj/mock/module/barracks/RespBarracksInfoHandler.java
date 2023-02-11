package org.protobj.mock.module.barracks;

import com.guangyu.cd003.projects.gs.module.barracks.msg.RespBarracksInfo;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespBarracksInfoHandler implements RespHandler<RespBarracksInfo> {

	@Override
	public void handle(MockConnect connect, RespBarracksInfo respMsg, int cmd) {
		//connect.LAST_RECV_MSGS.put(subCmd(), respMsg);
		connect.BARRACKS_DATA.handle(respMsg);
	}

	@Override
	public int subCmd() {
		return 1301;
	}
}
