package org.protobj.mock.ui.support;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.Marker;

import com.guangyu.cd003.projects.mock.ui.MockUIContext;

public class TextShowUtil{
	static MockUIContext ui;
	
	public static void bindUI(MockUIContext ui) {
		TextShowUtil.ui = ui;
	}
	
	public static void showContents(String contents) {
		if(ui != null) {
			ui.addText(contents);
		}
	}
	
	public static void showContents(String contents,Object... params) {
//		String.format(contents.replaceAll("\\{\\}", "%s"), params);
		TextShowUtil.showContents(String.format(contents, params));
	}
	
	static void showLoggerContents(String type,String clz,String contents,Object... params) {
		//[时间][类型][线程]
		StringBuilder sb = new StringBuilder();
		sb.append("[")
		.append(DateFormatUtils.format(System.currentTimeMillis(), "HH:mm:ss"))
//		sb.append(DateFormatUtils.format(System.currentTimeMillis(), "yyyy-MM-dd HH:mm:ss"));
		.append("][")
		.append(type)
		.append(",")
		.append(clz)
		.append("] ");
		if(contents.indexOf("%") > -1) {
			contents = contents.replaceAll("\\%", "%%");
		}
		sb.append(contents.replaceAll("\\{\\}", "%s"));
		TextShowUtil.showContents(String.format(sb.toString(), params));
	}
	
	public static LoggerUI creLogger(String name) {
		return new LoggerUI(name);
	}
	
	public static LoggerUI creLogger(Class<?> clz) {
		return new LoggerUI(clz);
	}
	
	
	static class LoggerUI implements Logger{
		static final String debug = "debug";
		static final String info = "info";
		static final String warn = "warn";
		static final String error = "error";
		private String clzName;
		
		public LoggerUI(Class<?> clz) {
			this.clzName = clz.getSimpleName();
		}

		public LoggerUI(String clzName) {
			this.clzName = clzName;
		}

		@Override
		public String getName() {
			return clzName;
		}

		@Override
		public boolean isTraceEnabled() {
			return false;
		}

		@Override
		public void trace(String msg) {
			
		}

		@Override
		public void trace(String format, Object arg) {
			
		}

		@Override
		public void trace(String format, Object arg1, Object arg2) {
			
		}

		@Override
		public void trace(String format, Object... arguments) {
			
		}

		@Override
		public void trace(String msg, Throwable t) {
			
		}

		@Override
		public boolean isTraceEnabled(Marker marker) {
			return false;
		}

		@Override
		public void trace(Marker marker, String msg) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void trace(Marker marker, String format, Object arg) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void trace(Marker marker, String format, Object arg1, Object arg2) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void trace(Marker marker, String format, Object... argArray) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void trace(Marker marker, String msg, Throwable t) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public boolean isDebugEnabled() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void debug(String msg) {
			showLoggerContents(LoggerUI.debug,getName(),msg);
		}

		@Override
		public void debug(String format, Object arg) {
			showLoggerContents(LoggerUI.debug,getName(),format,arg);
		}

		@Override
		public void debug(String format, Object arg1, Object arg2) {
			showLoggerContents(LoggerUI.debug,getName(),format,arg1,arg2);
		}

		@Override
		public void debug(String format, Object... arguments) {
			showLoggerContents(LoggerUI.debug,getName(),format,arguments);
		}

		@Override
		public void debug(String msg, Throwable t) {
			showLoggerContents(LoggerUI.debug,getName(),msg+"\t{}",ExceptionUtils.getStackTrace(t));
			t.printStackTrace();
		}

		@Override
		public boolean isDebugEnabled(Marker marker) {
			return false;
		}

		@Override
		public void debug(Marker marker, String msg) {
			
		}

		@Override
		public void debug(Marker marker, String format, Object arg) {
			
		}

		@Override
		public void debug(Marker marker, String format, Object arg1, Object arg2) {
			
		}

		@Override
		public void debug(Marker marker, String format, Object... arguments) {
			
		}

		@Override
		public void debug(Marker marker, String msg, Throwable t) {
			
		}

		@Override
		public boolean isInfoEnabled() {
			return false;
		}

		@Override
		public void info(String msg) {
			showLoggerContents(LoggerUI.info,getName(),msg);
		}

		@Override
		public void info(String format, Object arg) {
			showLoggerContents(LoggerUI.info,getName(),format,arg);
		}

		@Override
		public void info(String format, Object arg1, Object arg2) {
			showLoggerContents(LoggerUI.info,getName(),format,arg1,arg2);
		}

		@Override
		public void info(String format, Object... arguments) {
			showLoggerContents(LoggerUI.info,getName(),format,arguments);
		}

		@Override
		public void info(String msg, Throwable t) {
			showLoggerContents(LoggerUI.info,getName(),msg+"\t{}",ExceptionUtils.getStackTrace(t));
		}

		@Override
		public boolean isInfoEnabled(Marker marker) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void info(Marker marker, String msg) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void info(Marker marker, String format, Object arg) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void info(Marker marker, String format, Object arg1, Object arg2) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void info(Marker marker, String format, Object... arguments) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void info(Marker marker, String msg, Throwable t) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public boolean isWarnEnabled() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void warn(String msg) {
			showLoggerContents(LoggerUI.warn,getName(),msg);
		}

		@Override
		public void warn(String format, Object arg) {
			showLoggerContents(LoggerUI.warn,getName(),format,arg);
		}

		@Override
		public void warn(String format, Object... arguments) {
			showLoggerContents(LoggerUI.warn,getName(),format,arguments);
		}

		@Override
		public void warn(String format, Object arg1, Object arg2) {
			showLoggerContents(LoggerUI.warn,getName(),format,arg1,arg2);
		}

		@Override
		public void warn(String msg, Throwable t) {
			showLoggerContents(LoggerUI.warn,getName(),msg+"\t{}",ExceptionUtils.getStackTrace(t));
		}

		@Override
		public boolean isWarnEnabled(Marker marker) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void warn(Marker marker, String msg) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void warn(Marker marker, String format, Object arg) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void warn(Marker marker, String format, Object arg1, Object arg2) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void warn(Marker marker, String format, Object... arguments) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void warn(Marker marker, String msg, Throwable t) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public boolean isErrorEnabled() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void error(String msg) {
			showLoggerContents(LoggerUI.error,getName(),msg);
		}

		@Override
		public void error(String format, Object arg) {
			showLoggerContents(LoggerUI.error,getName(),format,arg);
		}

		@Override
		public void error(String format, Object arg1, Object arg2) {
			showLoggerContents(LoggerUI.error,getName(),format,arg1,arg2);
		}

		@Override
		public void error(String format, Object... arguments) {
			showLoggerContents(LoggerUI.error,getName(),format,arguments);
		}

		@Override
		public void error(String msg, Throwable t) {
			showLoggerContents(LoggerUI.error,getName(),msg+"\t{}",ExceptionUtils.getStackTrace(t));
		}

		@Override
		public boolean isErrorEnabled(Marker marker) {
			return false;
		}

		@Override
		public void error(Marker marker, String msg) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void error(Marker marker, String format, Object arg) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void error(Marker marker, String format, Object arg1, Object arg2) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void error(Marker marker, String format, Object... arguments) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void error(Marker marker, String msg, Throwable t) {
			// TODO Auto-generated method stub
			
		}
		
	}
}
