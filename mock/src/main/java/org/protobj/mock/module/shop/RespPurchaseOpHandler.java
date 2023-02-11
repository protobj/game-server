package org.protobj.mock.module.shop;

import com.guangyu.cd003.projects.gs.module.shop.msg.RespPurchaseOp;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespPurchaseOpHandler implements RespHandler<RespPurchaseOp> {

	@Override
	public void handle(MockConnect connect, RespPurchaseOp respMsg, int cmd) {
		connect.LAST_RECV_MSGS.put(subCmd(), respMsg);

	}

	@Override
	public int subCmd() {
		return 1703;
	}
}
