package org.protobj.mock.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * 弹出框
 * @author ChiangHo
 */
public class MockDialog extends JDialog implements WindowListener {
	private static final long serialVersionUID = 1L;
	private JTextArea area;
	private JButton okButton;
	private JButton cancelButton;

	private String rsString = "";

	private MockDialog(String title,int width,int height) {
		super();
		init();
		setModal(true);
		setSize(width, height);
		setIconImage(IcoMgr.FRAME_ICO);
		setTitle(title);
		addWindowListener(this);
	}

	private void init() {
		Container c = getContentPane();
		c.setLayout(new BorderLayout());
		area = new JTextArea();
		c.add(new JScrollPane(area));

		JPanel p = new JPanel();
		FlowLayout fl = new FlowLayout();
		fl.setHgap(15);
		p.setLayout(fl);
		okButton = new JButton("确定");
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				rsString = area.getText();
				dispose();
			}
		});

		cancelButton = new JButton("取消");
		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		p.add(okButton);
		p.add(cancelButton);

		c.add(p, BorderLayout.SOUTH);
	}

	public static String showDialog(Component relativeTo,String title,int width,int height, String text) {
		MockDialog d = new MockDialog(title,width,height);
		d.area.setText(text);
		d.rsString = text;
		if(relativeTo == null) {
			relativeTo = JOptionPane.getRootFrame();
		}
		d.setLocationRelativeTo(relativeTo);
		d.setVisible(true);
		return d.rsString;
	}
	
	public static String showDialog(String title,int width,int height,String text) {
		return showDialog(null,title,width,height, text);
	}
	
	public static String showDialog(String title,String text) {
		return showDialog(title,800,600, text);
	}

	@Override
	public void windowOpened(WindowEvent e) {
		
	}

	@Override
	public void windowClosing(WindowEvent e) {
		
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
