package org.protobj.mock.gui.netty;

import org.slf4j.Logger;

import com.guangyu.cd003.projects.mock.IMockContext;
import com.guangyu.cd003.projects.mock.MockNettyGwFrtndTmnlCommHandler;
import com.guangyu.cd003.projects.mock.ui.support.TextShowUtil;

public class MGCNettyGwFrtndTmnlHandler extends MockNettyGwFrtndTmnlCommHandler {
	static Logger logger = TextShowUtil.creLogger(MGCNettyGwFrtndTmnlHandler.class);
	public MGCNettyGwFrtndTmnlHandler(IMockContext mockContext) {
		super(mockContext);
	}
	
	@Override
	public Logger getLogger() {
		return logger;
	}
}
