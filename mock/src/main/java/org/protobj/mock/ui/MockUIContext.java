package org.protobj.mock.ui;

import java.awt.Color;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.swing.JMenuBar;
import javax.swing.JPanel;

import org.slf4j.Logger;

import com.guangyu.cd003.projects.mock.common.RunCmd;
import com.guangyu.cd003.projects.mock.ui.support.TextShowUtil;
import com.pv.common.utilities.common.CommonUtil;

public class MockUIContext implements RunCmd{
	MockFrame frame;
	MockTextArea textArea;
	JPanel buttonPanel;
	JMenuBar jmenuBar;
	List<MockButton> buts = CommonUtil.createList();
	static final Logger logger = TextShowUtil.creLogger(MockUIContext.class);
	String version = "3.0";
	String producer = "蒋豪、陈强";
	void init() {
		int offsetHeight = 39;
		frame = new MockFrame("Mock Game Client", 1000, 800);
		textArea = new MockTextArea(600, frame.getHeight()- offsetHeight,5000);
		textArea.addMock(frame);
		TextShowUtil.bindUI(this);
		buttonPanel = new JPanel();
		buttonPanel.setLayout(null);
//		buttonPanel.setBorder(BorderFactory.createLineBorder(Color.RED,1));
		buttonPanel.setBackground(new Color(220, 220, 220));
		buttonPanel.setBounds(textArea.getWidth(), 30, frame.getWidth() - textArea.getDwidth(),frame.getHeight()- offsetHeight);
		frame.add(buttonPanel);
		jmenuBar = new JMenuBar();
		jmenuBar.setBounds(textArea.getWidth(), 0, buttonPanel.getWidth(), 30);
		frame.add(jmenuBar);
		versionShow();
	}
	
	public void addText(String txt) {
		textArea.append(txt+"\r\n");
	}
	
	public void updateTitle(String title) {
		frame.setTitle(title);
	}
	
	public void addMenu(MockMenu menu) {
		this.jmenuBar.add(menu);
		repaint();
	}
	
	public void addBut(MockButton but) {
		but.setMockUIContext(this);
		buttonPanel.add(but);
		repaint();
		buts.add(but);
	}
	
	public void repaint() {
		buttonPanel.repaint();
		frame.validate();
		frame.repaint();
	}
	
	public MockButton getBtnBy(String name) {
		for(MockButton but :buts) {
			if(but.getText().equals(name)) {
				return but;
			}
		}
		return null;
	}
	
	public int countButs() {
		return this.buts.size();
	}
	
	public void clearButs() {
		for(MockButton but :buts) {
			buttonPanel.remove(but);
		}
		buts.clear();
		repaint();
	}
	
	public void startUI(){
		init();
		frame.showLook();
	}
	
	@Override
	public ExecutorService getServiceRun() {
		return frame.getServiceRun();
	}

	@Override
	public void runTask(Runnable run) {
		frame.runTask(run);
	}
	
	public MockFrame getFrameUI() {
		return this.frame;
	}
	
	private void versionShow() {
		logger.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>");
		logger.info("欢迎使用 Mock Game Client");
		logger.info("Version "+version);
		logger.info("开发人员："+producer);
		logger.info("<<<<<<<<<<<<<<<<<<<<<<<<<<<");
	}
}
