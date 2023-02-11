package org.protobj.mock.ui;

import java.util.concurrent.ExecutorService;

import javax.swing.JMenuItem;

import com.guangyu.cd003.projects.mock.common.RunCmd;

public class MockMenuItem extends JMenuItem implements RunCmd {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1477067049740703769L;
	
	MockUIContext mockUIContext;
	
	public MockMenuItem(String name) {
		super(name);
	}
	
	public MockMenuItem(String name,MockUIContext mockUIContext) {
		super(name);
		setMockUIContext(mockUIContext);
	}
	@Override
	public ExecutorService getServiceRun() {
		return mockUIContext.getServiceRun();	}
	@Override
	public void runTask(Runnable run) {
		mockUIContext.runTask(run);
	}
	public MockUIContext getMockUIContext() {
		return mockUIContext;
	}
	public void setMockUIContext(MockUIContext mockUIContext) {
		this.mockUIContext = mockUIContext;
	}
	
	
}
