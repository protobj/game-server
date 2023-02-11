package org.protobj.mock.module.quest;

import com.guangyu.cd003.projects.gs.module.quest.msg.RespQstInfo;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespQstInfoHandler implements RespHandler<RespQstInfo> {

	@Override
	public void handle(MockConnect connect, RespQstInfo respMsg, int cmd) {
		//connect.LAST_RECV_MSGS.put(subCmd(), respMsg);

	}

	@Override
	public int subCmd() {
		return 4501;
	}
}
