package org.protobj.mock.module.mail;

import com.guangyu.cd003.projects.gs.module.mail.msg.RespMailInfo;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespMailInfoHandler implements RespHandler<RespMailInfo> {

	@Override
	public void handle(MockConnect connect, RespMailInfo respMsg, int cmd) {
		//connect.LAST_RECV_MSGS.put(subCmd(), respMsg);
		connect.MAIL_DATA.handle(respMsg);
	}

	@Override
	public int subCmd() {
		return 901;
	}
}
