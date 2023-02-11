package org.protobj.mock.module.product;

import com.guangyu.cd003.projects.gs.module.product.msg.RespProductOp;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespProductOpHandler implements RespHandler<RespProductOp> {

	@Override
	public void handle(MockConnect connect, RespProductOp respMsg, int cmd) {
		//connect.LAST_RECV_MSGS.put(subCmd(), respMsg);
		connect.PRODUCT_DATA.handle(respMsg);
	}

	@Override
	public int subCmd() {
		return 1202;
	}
}
