package org.protobj.mock.common;

public class BlendedData {
	private int ana_Log_line_FileSize = 10485760;//解析错误日志属性字段：文件大小 默认10M
	private String ana_Log_line_startStr="[2022-06";//解析错误日志属性字段：一段完整异常的开始关键字 如：[2022-06
	private String ana_Log_line_errorSigns="java.lang,ERROR,Exception";//解析错误日志属性字段：异常关键字 如：java.lang,ERROR,Exception
	public String getAna_Log_line_startStr() {
		return ana_Log_line_startStr;
	}
	public void setAna_Log_line_startStr(String ana_Log_line_startStr) {
		this.ana_Log_line_startStr = ana_Log_line_startStr;
	}
	public String getAna_Log_line_errorSigns() {
		return ana_Log_line_errorSigns;
	}
	public void setAna_Log_line_errorSigns(String ana_Log_line_errorSigns) {
		this.ana_Log_line_errorSigns = ana_Log_line_errorSigns;
	}
	public int getAna_Log_line_FileSize() {
		return ana_Log_line_FileSize;
	}
	public void setAna_Log_line_FileSize(int ana_Log_line_FileSize) {
		this.ana_Log_line_FileSize = ana_Log_line_FileSize;
	}
}
