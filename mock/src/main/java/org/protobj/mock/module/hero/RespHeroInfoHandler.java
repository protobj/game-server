package org.protobj.mock.module.hero;

import com.guangyu.cd003.projects.gs.module.hero.msg.RespHeroInfo;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespHeroInfoHandler implements RespHandler<RespHeroInfo> {

	@Override
	public void handle(MockConnect connect, RespHeroInfo respMsg, int cmd) {
		//connect.LAST_RECV_MSGS.put(subCmd(), respMsg);
		connect.HERO_DATA.handle(respMsg);
	}

	@Override
	public int subCmd() {
		return 1601;
	}
}
