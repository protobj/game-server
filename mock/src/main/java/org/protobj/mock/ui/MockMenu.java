package org.protobj.mock.ui;

import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import javax.swing.JMenu;

import org.slf4j.Logger;

import com.guangyu.cd003.projects.mock.common.RunCmd;
import com.guangyu.cd003.projects.mock.ui.support.TextShowUtil;

public class MockMenu extends JMenu implements RunCmd{
	static final Logger logger = TextShowUtil.creLogger(MockMenu.class);
	private static final long serialVersionUID = 5163724833254383480L;
	MockUIContext mockUIContext;
	public MockMenu(String name,MockUIContext mockUIContext) {
		super(name);
		setMockUIContext(mockUIContext);
	}
	public MockUIContext getMockUIContext() {
		return mockUIContext;
	}
	public void setMockUIContext(MockUIContext mockUIContext) {
		this.mockUIContext = mockUIContext;
	}
	@Override
	public ExecutorService getServiceRun() {
		return mockUIContext.getServiceRun();	}
	@Override
	public void runTask(Runnable run) {
		mockUIContext.runTask(run);
	}
	
	public MockMenu addItem(MockMenuItem item,Consumer<MockMenuItem> listenAction) {
		item.setMockUIContext(mockUIContext);
		item.addActionListener((e)->{
			try {
				listenAction.accept((MockMenuItem)e.getSource());
			}catch (Exception ex) {
				logger.error("",ex);
			}
		});
		add(item);
		return this;
	}
	
	public void addToUI() {
		mockUIContext.addMenu(this);
	}
}
