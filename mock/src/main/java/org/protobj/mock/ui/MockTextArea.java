package org.protobj.mock.ui;

import java.awt.Color;
import java.awt.Font;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;

import com.guangyu.cd003.projects.mock.common.RunCmd;

public class MockTextArea extends JTextArea implements RunCmd {
	private static final long serialVersionUID = -4139168178722964131L;
	private JScrollPane jsp;
	private MockFrame frame;
	private int maxLine;
	private ExecutorService showTxt = Executors.newFixedThreadPool(1);
	public  MockTextArea(int width,int height,int maxLine) {
		 setEditable(false);
		 setLineWrap(true);
		 setSize(width, height);
		 setForeground(Color.BLUE);
		 setBackground(new Color(220, 220, 220));
		 setFont(new Font("monospaced", Font.PLAIN, 12));
		 DefaultCaret caret = (DefaultCaret)getCaret();
		 caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		 this.maxLine = maxLine;
	}
	
	public void addMock(MockFrame frame) {
		 jsp = new JScrollPane(this);
//		 jsp.setBorder(BorderFactory.createLineBorder(Color.RED,1));
		 jsp.setSize(getWidth(), getHeight());
		 jsp.setBounds(0, 0, getWidth(), getHeight());
		 jsp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		 initListen();
		 this.frame = frame;
		 frame.add(jsp);
	}
	
	public int getDwidth() {
		return jsp.getWidth();
	}
	void initListen() {
		getDocument().addDocumentListener(new TextDocumentListener(this));
	}
	
	
	@Override
	public void append(String str) {
		showTxt.execute(()->{
			super.append(str);
			MockTextArea textArea = MockTextArea.this;
			textArea.setCaretPosition(textArea.getDocument().getLength()); 
//			textArea.paintImmediately(textArea.getX(), textArea.getY(), textArea.getWidth(), textArea.getHeight());
		});
	}


	class TextDocumentListener implements DocumentListener,Runnable{
		private MockTextArea  textArea;
		
		public TextDocumentListener(MockTextArea textArea) {
			this.textArea = textArea;
		}

		@Override
		public void run() {
			try {
				int num = textArea.getLineCount() - textArea.maxLine;
				if(num > 0) {
					int end = textArea.getLineEndOffset(num - 1);
					textArea.replaceRange("", 0, end);
				}
			} catch (BadLocationException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void insertUpdate(DocumentEvent e) {
			if(textArea.getLineCount() > textArea.maxLine) {
				textArea.showTxt.execute(this);
			}
		}

		@Override
		public void removeUpdate(DocumentEvent e) {
			
		}

		@Override
		public void changedUpdate(DocumentEvent e) {
			
		}
		
	}

	@Override
	public ExecutorService getServiceRun() {
		return this.frame.getServiceRun();
	}

	@Override
	public void runTask(Runnable run) {
		this.frame.runTask(run);
	}

}
