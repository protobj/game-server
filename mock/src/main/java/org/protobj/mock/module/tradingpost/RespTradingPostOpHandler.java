package org.protobj.mock.module.tradingpost;

import com.guangyu.cd003.projects.gs.module.tradingpost.msg.RespTradingPostOp;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespTradingPostOpHandler implements RespHandler<RespTradingPostOp> {

	@Override
	public void handle(MockConnect connect, RespTradingPostOp respMsg, int cmd) {
		//connect.LAST_RECV_MSGS.put(subCmd(), respMsg);

	}

	@Override
	public int subCmd() {
		return 4302;
	}
}
