package org.protobj.mock.module.licence;

import com.guangyu.cd003.projects.gs.module.licence.msg.RespLicenceOp;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespLicenceOpHandler implements RespHandler<RespLicenceOp> {

	@Override
	public void handle(MockConnect connect, RespLicenceOp respMsg, int cmd) {
		connect.LAST_RECV_MSGS.put(subCmd(), respMsg);

	}

	@Override
	public int subCmd() {
		return 5602;
	}
}
