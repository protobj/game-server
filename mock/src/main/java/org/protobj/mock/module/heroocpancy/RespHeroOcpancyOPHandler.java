package org.protobj.mock.module.heroocpancy;

import com.guangyu.cd003.projects.gs.module.heroocpancy.msg.RespHeroOcpancyOP;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespHeroOcpancyOPHandler implements RespHandler<RespHeroOcpancyOP> {

	@Override
	public void handle(MockConnect connect, RespHeroOcpancyOP respMsg, int cmd) {
		//connect.LAST_RECV_MSGS.put(subCmd(), respMsg);

	}

	@Override
	public int subCmd() {
		return 3702;
	}
}
