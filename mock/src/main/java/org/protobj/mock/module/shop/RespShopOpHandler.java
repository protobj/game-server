package org.protobj.mock.module.shop;

import com.guangyu.cd003.projects.gs.module.shop.msg.RespShopOp;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespShopOpHandler implements RespHandler<RespShopOp> {

	@Override
	public void handle(MockConnect connect, RespShopOp respMsg, int cmd) {
		//connect.LAST_RECV_MSGS.put(subCmd(), respMsg);

	}

	@Override
	public int subCmd() {
		return 1701;
	}
}
