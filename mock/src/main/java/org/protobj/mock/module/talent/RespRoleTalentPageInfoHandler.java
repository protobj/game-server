package org.protobj.mock.module.talent;

import com.guangyu.cd003.projects.gs.module.talent.msg.RespRoleTalentPageInfo;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespRoleTalentPageInfoHandler implements RespHandler<RespRoleTalentPageInfo> {

	@Override
	public void handle(MockConnect connect, RespRoleTalentPageInfo respMsg, int cmd) {
		//connect.LAST_RECV_MSGS.put(subCmd(), respMsg);

	}

	@Override
	public int subCmd() {
		return 4001;
	}
}
