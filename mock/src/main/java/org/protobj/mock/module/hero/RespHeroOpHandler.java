package org.protobj.mock.module.hero;

import com.guangyu.cd003.projects.gs.module.hero.msg.RespHeroOp;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespHeroOpHandler implements RespHandler<RespHeroOp> {

	@Override
	public void handle(MockConnect connect, RespHeroOp respMsg, int cmd) {
		//connect.LAST_RECV_MSGS.put(subCmd(), respMsg);
		connect.HERO_DATA.handle(respMsg);
	}

	@Override
	public int subCmd() {
		return 1602;
	}
}
