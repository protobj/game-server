package org.protobj.mock.common;

import com.alibaba.fastjson.JSON;

public class MockGameCmd {
	private int cmd;
	private String cmdName;
	private String msg;
	public int getCmd() {
		return cmd;
	}
	public void setCmd(int cmd) {
		this.cmd = cmd;
	}
	public String getCmdName() {
		return cmdName;
	}
	public void setCmdName(String cmdName) {
		this.cmdName = cmdName;
	}
	public String getMsg() {
		return msg;
	}
	public void setMsg(String msg) {
		this.msg = msg;
	}
	public <T> T getMsgBy(Class<T> msgClz) {
		return JSON.parseObject(msg, msgClz);
	}
}
