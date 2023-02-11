package org.protobj.mock.module.lgscientific;

import com.guangyu.cd003.projects.gs.module.lgscientific.msg.RespLgScientificInfo;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespLgScientificInfoHandler implements RespHandler<RespLgScientificInfo> {

	@Override
	public void handle(MockConnect connect, RespLgScientificInfo respMsg, int cmd) {
		//connect.LAST_RECV_MSGS.put(subCmd(), respMsg);

	}

	@Override
	public int subCmd() {
		return 3101;
	}
}
