package org.protobj.mock.gui.load.proc;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.curator.shaded.com.google.common.base.Charsets;
import org.slf4j.Logger;

import com.guangyu.cd003.projects.mock.common.BlendedData;
import com.guangyu.cd003.projects.mock.ui.support.TextShowUtil;
import com.pv.common.utilities.common.CommonUtil;
import com.pv.common.utilities.common.StringUtil;

/**
 * 解析日志文件提取错误信息
 * @author ChiangHo
 */
public class AnalysisLogErrorInfo extends Thread {
	static final Logger logger = TextShowUtil.creLogger(AnalysisLogErrorInfo.class);
	private File sourceFile;//源文件
	private BlendedData blendedData;
	private String sourcePath;
	private String sourceFileName;
	private int splitFileSize;
	private String lineStartStr;
	private String[] errorSigns;
	private String lineSymbol;
	public AnalysisLogErrorInfo(File sourceFile,BlendedData blendedData) {
		this.sourceFile = sourceFile;
		this.blendedData =  blendedData;
		init();
	}
	
	@SuppressWarnings("restriction")
	private void init() {
		String absolutePath = sourceFile.getAbsolutePath();
		this.sourcePath = absolutePath.substring(0, absolutePath.lastIndexOf(File.separator));
		this.sourceFileName = sourceFile.getName();
		this.lineSymbol = java.security.AccessController.doPrivileged(
				            new sun.security.action.GetPropertyAction("line.separator"));
		this.splitFileSize = blendedData.getAna_Log_line_FileSize();
		this.lineStartStr = blendedData.getAna_Log_line_startStr();
		this.errorSigns = blendedData.getAna_Log_line_errorSigns().trim().split(",");
	}
	
	@Override
	public void run() {
		try(
			LineIterator lineIterator = FileUtils.lineIterator(Paths.get(this.sourcePath , this.sourceFileName).toFile());
			SplitFileWrite splitOutInner = new SplitFileWrite(sourcePath+"/"+sourceFileName+"s", sourceFileName, splitFileSize);
//			MergeFileWrite mergeWrite = new MergeFileWrite(this, splitOutInner);
				){
			LineStrMarge mergeLine = new LineStrMarge(this.lineSymbol,this.lineStartStr,txt -> {
				for (int i = 0; i < errorSigns.length; i++) {
					if(txt.indexOf(errorSigns[i]) > -1) {
						return true;
					}
				}
				return false;
			});
			while(lineIterator.hasNext()) {
				String nextLine = lineIterator.nextLine().trim();
				String result = mergeLine.collect(nextLine);
				if(StringUtil.isNotEmpty(result)) {
					splitOutInner.write(result);
				}
			}
			String resultLast = mergeLine.collectLast();
			if(StringUtil.isNotEmpty(resultLast)) {
				splitOutInner.write(resultLast);
			}
//			mergeWrite.startMerge();
		}catch (Exception e) {
			logger.error("e",e);
		}
		logger.info("====解析完毕===");
	}
	
	static class LineStrMarge{
		private StringBuilder errLine = new StringBuilder();
		private String lineSymbol;
		private String lineStartStr;
		private Predicate<String> filter;
		public LineStrMarge(String lineSymbol, String lineStartStr,Predicate<String> filter) {
			this.lineSymbol = lineSymbol;
			this.lineStartStr = lineStartStr;
			this.filter = filter;
		}

		/**
		 * 收集零散信息 组成一段完整的信息
		 * @param txt 
		 * @return string
		 */
		public String  collect(String txt) {
			if(txt.startsWith(lineStartStr)) {
				String result = errLine.toString();
				errLine.delete(0, errLine.length());
				errLine.append(txt).append(this.lineSymbol);
				if(filter != null && filter.test(result)) {
					return result;
				}
				return null;
			}
			errLine.append(txt).append(this.lineSymbol);
			return null;
		}
		
		/**
		 * 获取最后一段信息
		 * @return
		 */
		public String collectLast() {
			String result =  collect(lineStartStr);
			errLine.delete(0, errLine.length());
			return result;
		}
		
	}
	
	/**
	 * 文件分片存储
	 * @author ChiangHo
	 */
	static class SplitFileWrite implements Closeable{
		BufferedWriter bufWrite;
		String path;
		boolean mkState;
		String fileName;
		Path outPath;
		ExecutorService newFixedThreadPool;
		Charset charType;
		Progress fileMaxSizeProgress;
		int splitIndex;
		Progress progLineProgress;
		List<Path> splitFileNames;
		String splitLineSymbol = "==&line&==";
		public SplitFileWrite(String path,String fileName,int fileSize, String charType) {
			this.path = path;
			this.fileName = fileName+"."+DateFormatUtils.format(new Date(), "HH_mm_ss");
			this.newFixedThreadPool = Executors.newFixedThreadPool(1);
			this.charType = Charset.forName(charType);
			this.fileMaxSizeProgress = new Progress(fileSize);//2M
			this.progLineProgress = new Progress();
		}
		
		public SplitFileWrite(String path,String fileName,int fileSize) {
			this(path, fileName,fileSize,"utf-8");
		}
		
		public void write(String txt) {
			progLineProgress.incrMaxProNum(1);
			newFixedThreadPool.execute(()->{
				try {
					requireBufferedWriter();
					bufWrite.write(txt);
					bufWrite.write(splitLineSymbol);
					bufWrite.newLine();
					splitFile(txt);
					progLineProgress.incrProcNoCheck(1);
					if(progLineProgress.getMaxProNum() % 10 == 0)
						logger.info("进度：{}/{}",progLineProgress.getCurProNum(),progLineProgress.getMaxProNum());
				} catch (IOException e) {
					logger.error("e",e);
				}
			});
		}
		
		private void splitFile(String txt) {
			this.fileMaxSizeProgress.incrProc(txt.getBytes(charType).length);
			if(!this.fileMaxSizeProgress.isComplete()) {
				return;
			}
			this.fileMaxSizeProgress.reset();
			splitIndex ++;
			try {
				this.bufWrite.flush();
				this.bufWrite.close();
				this.outPath = null;
				this.bufWrite = null;
			} catch (IOException e) {
				logger.error("e",e);
			}
		}
		
		private void requireBufferedWriter() {
			if(bufWrite == null) {
				try {
					requireFile();
					requireFileMk();
					bufWrite = java.nio.file.Files.newBufferedWriter(outPath, charType, StandardOpenOption.APPEND,StandardOpenOption.CREATE_NEW,StandardOpenOption.CREATE);
					logger.info("create file:{}",outPath.getFileName());
//					bufWrite = Files.newWriter(outPath.toFile(), charType);
				} catch (IOException e) {
					logger.error("e",e);
				} 
			}
		}
		
		private void requireFileMk() {
			if(mkState)return;
			File file = new File(path);
			if(!file.exists()) {
					file.mkdirs();
			}
			mkState = true;
		}
		
		private void requireFile() {
			if(outPath == null) {
				requireFileNames();
				outPath = Paths.get(path,this.fileName+"."+splitIndex);
				this.splitFileNames.add(outPath);
			}
		}
		
		private void requireFileNames() {
			if(this.splitFileNames == null) {
				this.splitFileNames = CommonUtil.createList();
			}
		}
		
		
		public List<Path> getSplitFileNames() {
			return splitFileNames;
		}
		
		@Override
		public void close() throws IOException {
			newFixedThreadPool.shutdown();
			try {
				newFixedThreadPool.awaitTermination(3, TimeUnit.HOURS);
			} catch (InterruptedException e) {
				logger.error("e",e);
			}finally {
				newFixedThreadPool.shutdownNow();
			}
			logger.info("进度：{}/{}",progLineProgress.getCurProNum(),progLineProgress.getMaxProNum());
			if(bufWrite != null) {
				bufWrite.close();
			}
		}
	}
	
	/**
	 * 文本合并 TODO 不现实太乱了每个细节都不一样
	 * @author ChiangHo
	 */
	static class MergeFileWrite implements Closeable{
		private ExecutorService newFixedThreadPool;
		private AnalysisLogErrorInfo analysis;
		private SplitFileWrite splitFileWrite;
		public MergeFileWrite(AnalysisLogErrorInfo analysis,SplitFileWrite splitFileWrite) {
			super();
			this.splitFileWrite = splitFileWrite;
			this.newFixedThreadPool = Executors.newFixedThreadPool(5); //Cpu数量 * 2
			this.analysis = analysis;
		}
		
		/**
		 * 开始合并
		 */
		public void startMerge() {
			splitFileWrite.getSplitFileNames().forEach((path)->{
				this.newFixedThreadPool.execute(()->{
					repeatContent(path);
				});
			});
		}
		
		private Path repeatContent(Path path) {
			logger.info("start meger File:{}",path.getFileName());
			LineStrMarge mergeLine = new LineStrMarge(analysis.lineSymbol,splitFileWrite.splitLineSymbol,null);
			Map<Integer, Set<String>> mergeTxt = CommonUtil.createSimpleMap();
			try(LineIterator lineIterator = FileUtils.lineIterator(path.toFile());){
				while(lineIterator.hasNext()) {
					String collectTxt = mergeLine.collect(lineIterator.next());
					if(StringUtils.isNotEmpty(collectTxt)) {
						megerProc(mergeTxt, collectTxt);
					}
				}
				String collectTxt = mergeLine.collectLast();
				if(StringUtils.isNotEmpty(collectTxt)) {
					megerProc(mergeTxt, collectTxt);
				}
			}catch (Exception e) {
				logger.error("e",e);
			}
			writeFIle(path, mergeTxt);
			return path;
		}
		
		private void writeFIle(Path path,Map<Integer, Set<String>> mergeTxt) {
			requireFileDel(path);
			try(BufferedWriter bufWrite = java.nio.file.Files.newBufferedWriter(path, Charsets.UTF_8, StandardOpenOption.APPEND,StandardOpenOption.CREATE_NEW,StandardOpenOption.CREATE);){
				mergeTxt.entrySet().stream().forEach((ets)->{
					Set<String> value = ets.getValue();
					value.forEach(t -> {
						try {
							bufWrite.write(t);
						} catch (IOException e) {
							logger.error("e",e);
						}
					});
				});
			}catch (Exception e) {
				logger.error("e",e);
			}
		}
		
		
		private void requireFileDel(Path path) {
			File file = path.toFile();
			if(file.exists()) {
				file.delete();
			}
		}
		
		private void megerProc(Map<Integer, Set<String>> mergeTxt,String collectTxt) {
			String subNextTxt = collectTxt.substring(25, collectTxt.length());
			mergeTxt.compute(subNextTxt.hashCode(),(key,value)->{
				if(value == null) {
					value = CommonUtil.createSet();
					value.add(collectTxt);
				}else {
					for (String nvalue : value) {
						String subNvalue = nvalue.substring(25, nvalue.length());//节约点内存
						if(!subNvalue.equals(subNextTxt)) {
							value.add(collectTxt);
						}
					}
				}
				return value;
			});
		}
		@Override
		public void close() throws IOException {
			this.newFixedThreadPool.shutdown();
			try {
				this.newFixedThreadPool.awaitTermination(3, TimeUnit.HOURS);
			} catch (InterruptedException e) {
				logger.error("e",e);
			}finally {
				this.newFixedThreadPool.shutdownNow();
			}
		}
		
	}
	
	public static  class Progress{
		private int maxProNum;
		private volatile int curProNum;
		public Progress(int maxProNum) {
			this.maxProNum = maxProNum;
		}
		
		public Progress() {
			
		}
		
		public void incrProc(int incr) {
			this.curProNum += incr;
			checkSetMax();
		}
		
		public void incrProc() {
			this.curProNum ++;
			checkSetMax();
		}
		
		public void incrMaxProNum(int incr) {
			this.maxProNum += incr;
		}
		
		public void incrProcNoCheck(int incr) {
			this.curProNum += incr;
		}
		
		private void checkSetMax() {
			if(this.curProNum > maxProNum) {
				this.curProNum = maxProNum;
			}
		}
		
		public boolean isComplete() {
			return this.curProNum >= this.maxProNum;
		}
		
		public void reset() {
			this.curProNum  = 0;
		}
		
		public int p() {
			return this.curProNum * 100 / this.maxProNum;
		}

		public int getMaxProNum() {
			return maxProNum;
		}

		public int getCurProNum() {
			return curProNum;
		}
		
	}
}
