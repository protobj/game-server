package org.protobj.mock.module.mail;

import com.guangyu.cd003.projects.gs.module.mail.msg.RespMailOp;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespMailOpHandler implements RespHandler<RespMailOp> {

	@Override
	public void handle(MockConnect connect, RespMailOp respMsg, int cmd) {
		//connect.LAST_RECV_MSGS.put(subCmd(), respMsg);
		connect.MAIL_DATA.handle(respMsg);
	}

	@Override
	public int subCmd() {
		return 902;
	}
}
