package org.protobj.mock.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import javax.swing.JButton;

import org.slf4j.Logger;

import com.guangyu.cd003.projects.mock.common.RunCmd;
import com.guangyu.cd003.projects.mock.ui.support.TextShowUtil;

public class MockButton extends JButton implements RunCmd,ActionListener {
	private static final long serialVersionUID = -778999978407815893L;
	static final Logger logger = TextShowUtil.creLogger(MockButton.class);
	MockUIContext mockUIContext;
	Consumer<MockButton> listenAction;
	public MockButton(String title,Consumer<MockButton> listenAction) {
		this.listenAction = listenAction;
		setText(title);
		setSize(130, 30);
		addActionListener(this);
	}
	
	@Override
	public ExecutorService getServiceRun() {
		return getMockUIContext().getServiceRun();
	}

	@Override
	public void runTask(Runnable run) {
		getMockUIContext().runTask(run); 
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		try {
			this.listenAction.accept(this);
		}catch (Exception ex) {
			setEnabled(true);
			logger.error("",ex);
		}
	}

	public MockUIContext getMockUIContext() {
		return mockUIContext;
	}

	public void setMockUIContext(MockUIContext mockUIContext) {
		this.mockUIContext = mockUIContext;
	}
	
}
