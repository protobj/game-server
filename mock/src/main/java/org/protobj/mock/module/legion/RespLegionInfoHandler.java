package org.protobj.mock.module.legion;

import com.guangyu.cd003.projects.gs.module.legion.cons.RespTypeLegion;
import com.guangyu.cd003.projects.gs.module.legion.msg.RespLegionInfo;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespLegionInfoHandler implements RespHandler<RespLegionInfo> {

	@Override
	public void handle(MockConnect connect, RespLegionInfo respMsg, int cmd) {
		//connect.LAST_RECV_MSGS.put(subCmd(), respMsg);
		connect.LEGION_DATA.handle(respMsg);
	}

	@Override
	public int subCmd() {
		return RespTypeLegion.RESP_TYPE_LEGION_INFO;
	}
}
