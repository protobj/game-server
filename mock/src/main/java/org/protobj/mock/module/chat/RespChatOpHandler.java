package org.protobj.mock.module.chat;

import com.guangyu.cd003.projects.gs.module.chat.msg.RespChatOp;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespChatOpHandler implements RespHandler<RespChatOp> {

	@Override
	public void handle(MockConnect connect, RespChatOp respMsg, int cmd) {
		//connect.LAST_RECV_MSGS.put(subCmd(), respMsg);

	}

	@Override
	public int subCmd() {
		return 1001;
	}
}
