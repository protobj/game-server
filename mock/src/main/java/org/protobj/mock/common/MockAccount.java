package org.protobj.mock.common;

public class MockAccount {
	private String uid="1";
	private String serverId="222";
	private String[] gateWay = new String[] {"dev14.com:8199", "dev14.com:8199"};
	public String getUid() {
		return uid;
	}
	public void setUid(String uid) {
		this.uid = uid;
	}
	public String getServerId() {
		return serverId;
	}
	public void setServerId(String serverId) {
		this.serverId = serverId;
	}
	public String[] getGateWay() {
		return gateWay;
	}
	public void setGateWay(String[] gateWay) {
		this.gateWay = gateWay;
	}
}
