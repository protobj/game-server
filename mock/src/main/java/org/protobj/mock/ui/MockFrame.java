package org.protobj.mock.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JFrame;

import com.guangyu.cd003.projects.mock.common.RunCmd;

public class MockFrame extends JFrame implements RunCmd,WindowListener{
	private static final long serialVersionUID = 8286665821719774258L;
	private ExecutorService serviceRun;
	
	public MockFrame(String name,int width,int height) {
		setTitle(name);
		setLayout(null);
		setSize(width, height);
		setPreferredSize(new Dimension(width, height));
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocationRelativeTo(null);
		setResizable(false);
		setBackground(new Color(220, 220, 220));
		setIconImage(IcoMgr.FRAME_ICO);
		serviceRun = Executors.newFixedThreadPool(1);
		addWindowListener(this);
	}
	
	public ExecutorService getServiceRun() {return serviceRun;};
	
	public void runTask(Runnable run) {
		serviceRun.execute(run);
	}
	
	public void showLook() {
		pack();
		setVisible(true);
	}

	@Override
	public void windowOpened(WindowEvent e) {
		
	}

	@Override
	public void windowClosing(WindowEvent e) {
		System.exit(0);
	}

	@Override
	public void windowClosed(WindowEvent e) {
		
	}

	@Override
	public void windowIconified(WindowEvent e) {
		
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
		
	}

	@Override
	public void windowActivated(WindowEvent e) {
		
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
		
	}
}
