package org.protobj.mock.common;

import java.util.concurrent.ExecutorService;

public interface RunCmd {
	ExecutorService getServiceRun();
	void runTask(Runnable run);
}
