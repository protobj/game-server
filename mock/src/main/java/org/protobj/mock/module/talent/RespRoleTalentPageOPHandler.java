package org.protobj.mock.module.talent;

import com.guangyu.cd003.projects.gs.module.talent.msg.RespRoleTalentPageOP;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespRoleTalentPageOPHandler implements RespHandler<RespRoleTalentPageOP> {

	@Override
	public void handle(MockConnect connect, RespRoleTalentPageOP respMsg, int cmd) {
		//connect.LAST_RECV_MSGS.put(subCmd(), respMsg);

	}

	@Override
	public int subCmd() {
		return 4002;
	}
}
