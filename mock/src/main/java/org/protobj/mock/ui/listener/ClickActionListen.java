package org.protobj.mock.ui.listener;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import com.guangyu.cd003.projects.mock.ui.MockButton;

public class ClickActionListen implements ActionListener {
	private MockButton buttion;
	
	public ClickActionListen(MockButton buttion) {
		this.buttion = buttion;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		
	}

}
