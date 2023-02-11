package org.protobj.mock.module.language;

import com.guangyu.cd003.projects.gs.module.language.msg.RespLang;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespLangHandler implements RespHandler<RespLang> {

	@Override
	public void handle(MockConnect connect, RespLang respMsg, int cmd) {
		//connect.LAST_RECV_MSGS.put(subCmd(), respMsg);

	}

	@Override
	public int subCmd() {
		return 2601;
	}
}
