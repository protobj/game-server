package org.protobj.mock.module.scene;

import com.guangyu.cd003.projects.gs.module.scene.msg.RespSceneOp;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.RespHandler;

public class RespSceneOpHandler implements RespHandler<RespSceneOp> {

	@Override
	public void handle(MockConnect connect, RespSceneOp respMsg, int cmd) {
		//connect.LAST_RECV_MSGS.put(subCmd(), respMsg);
		connect.SCENE_DATA.handle(connect,respMsg);
	}

	@Override
	public int subCmd() {
		return 302;
	}
}
