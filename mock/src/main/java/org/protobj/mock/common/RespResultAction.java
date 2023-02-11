package org.protobj.mock.common;

import com.guangyu.cd003.projects.mock.HandlerResult;
import com.guangyu.cd003.projects.mock.gui.bo.MGCConnect;

public interface RespResultAction {
	void action(MGCConnect connect,HandlerResult result,Object resp, int code);
}
