package org.protobj.mock.module.lgscientific;

import com.guangyu.cd003.projects.gs.module.lgscientific.msg.RespLgScientificSkillOP;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespLgScientificSkillOPHandler implements RespHandler<RespLgScientificSkillOP> {

	@Override
	public void handle(MockConnect connect, RespLgScientificSkillOP respMsg, int cmd) {
		//connect.LAST_RECV_MSGS.put(subCmd(), respMsg);

	}

	@Override
	public int subCmd() {
		return 3103;
	}
}
