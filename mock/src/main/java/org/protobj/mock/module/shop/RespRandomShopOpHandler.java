package org.protobj.mock.module.shop;

import com.guangyu.cd003.projects.gs.module.shop.msg.RespRandomShopOp;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespRandomShopOpHandler implements RespHandler<RespRandomShopOp> {

	@Override
	public void handle(MockConnect connect, RespRandomShopOp respMsg, int cmd) {
		//connect.LAST_RECV_MSGS.put(subCmd(), respMsg);

	}

	@Override
	public int subCmd() {
		return 1702;
	}
}
