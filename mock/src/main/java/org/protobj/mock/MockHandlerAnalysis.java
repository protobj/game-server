package org.protobj.mock;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.pv.framework.gs.core.module.IModuleInfo;
import org.apache.commons.collections4.map.HashedMap;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import com.guangyu.cd003.projects.common.msg.RespRawDataType;
import com.guangyu.cd003.projects.mock.common.BlendedData;
import com.guangyu.cd003.projects.mock.common.ConstMockUI;
import com.guangyu.cd003.projects.mock.common.MockAccount;
import com.guangyu.cd003.projects.mock.common.RespResultAction;
import com.guangyu.cd003.projects.mock.ui.support.TextShowUtil;
import com.pv.common.utilities.common.StringUtil;
import com.pv.framework.gs.core.module.annotation.CliMsgMethod;
import com.pv.framework.gs.core.module.msgproc.NullRqstMsg;

public class MockHandlerAnalysis{
	static final Logger logger = TextShowUtil.creLogger(MockHandlerAnalysis.class);
	static final String pkg = "com.guangyu.cd003.projects.gs.module.";
	static final String fileDBName = "mockHandler.db";
	private Set<String> creatorModules;
	private Map<Integer, RestHanlderParam> code_paramMap;
	private Map<Integer, RespHanlderParam> resp_map;
	private MockAccount account;
	private BlendedData blendedData;
	public MockHandlerAnalysis() {
		this.creatorModules = new HashSet<>();
		this.code_paramMap = new HashedMap<>();
		this.resp_map = new HashedMap<>();
		this.account = new MockAccount();
		this.blendedData = new BlendedData();
	}

	public void loadHandler() {
		code_paramMap.clear();
		resp_map.clear();
		creatorModules.clear();
		Reflections reflections = new Reflections(new ConfigurationBuilder()
				.forPackages(pkg.substring(0,pkg.length()))
		);
		int sidx = pkg.length();
		Set<Class<? extends IModuleInfo>> subTypesOf = reflections.getSubTypesOf(IModuleInfo.class);
		for (Class<?> sub : subTypesOf) {
			String name = sub.getName();
			String creatorModule = name.substring(name.indexOf(pkg) + sidx, name.lastIndexOf("."));
			this.creatorModules.add(creatorModule.trim());
		}
		for (String creatorModule : creatorModules) {
			analysisParam(creatorModule);
        }
		save();
	}
	
	private void analysisParam(String module) {
		String analysisPkg = pkg + module;
		logger.info("start analysisParam pkg:{}",analysisPkg);
		Reflections reflections = new Reflections(new ConfigurationBuilder()
                .forPackages(analysisPkg + ".")
                .addScanners(Scanners.MethodsAnnotated)
                .addScanners(Scanners.TypesAnnotated)
        );
		Collection<Method> methodsAnnotatedWith = reflections.getMethodsAnnotatedWith(CliMsgMethod.class);
		List<String> methodNames = new ArrayList<>();
        String className = null;
        //请求方法
        methodsAnnotatedWith = methodsAnnotatedWith.stream().filter(t -> t.getAnnotation(CliMsgMethod.class) != null).sorted(Comparator.comparing(t -> {
            return t.getAnnotation(CliMsgMethod.class).value();
        })).collect(Collectors.toList());
        for (Method method : methodsAnnotatedWith) {
            if (!method.getDeclaringClass().getName().startsWith(analysisPkg + ".")) {
                continue;
            }
            CliMsgMethod annotation = method.getAnnotation(CliMsgMethod.class);
            Class aClass = annotation.rqstType();
            if (aClass == NullRqstMsg.class) {
            	 String rqstMethodName = method.getName() + "_" + annotation.value();
            	code_paramMap.put(annotation.value(), new RestHanlderParam(rqstMethodName, annotation.value(), className));
            }else {
            	String simpleName = aClass.getSimpleName();
                String rqstMethodName = simpleName + "_" + annotation.value();
                code_paramMap.put(annotation.value(), new RestHanlderParam(rqstMethodName, annotation.value(),aClass.getName()));
            }
        }
        //返回方法
        Collection<Class<?>> respClass = reflections.getTypesAnnotatedWith(RespRawDataType.class);
        respClass = respClass.stream().filter(t -> t.getAnnotation(RespRawDataType.class) != null)
                .sorted(Comparator.comparing(t -> t.getAnnotation(RespRawDataType.class).value()))
                .collect(Collectors.toList());
        for (Class<?> aClass : respClass) {
            if (!aClass.getName().startsWith(analysisPkg + ".")) {
                continue;
            }
            RespRawDataType annotation = aClass.getAnnotation(RespRawDataType.class);
            resp_map.put(annotation.value(), new RespHanlderParam(annotation.value(), aClass.getSimpleName(), aClass.getName()));
        }
	}
	
	public RestHanlderParam getCodeParamBy(Integer code) {
		return this.code_paramMap.get(code);
	}
	
	public RespHanlderParam getRespParamBy(Integer code) {
		return this.resp_map.get(code);
	}
	
	public void regstRespAction(Integer code,RespResultAction action) {
		RespHanlderParam objects = resp_map.get(code);
		if(objects != null) {
			objects.setAction(action);
			logger.info("regstRespAction cmd:{}",code);
		}else {
			resp_map.put(code, new RespHanlderParam(code, "", "",action));
			logger.info("regstRespAction cmd:{}",code);
		}
	}
	
	public RespResultAction getRespAction(Integer code) {
		RespHanlderParam objects = resp_map.get(code);
		if(objects != null) {
			return objects.getAction();
		}
		return null;
	}

	public Set<String> getCreatorModules() {
		return creatorModules;
	}

	public void setCreatorModules(Set<String> creatorModules) {
		this.creatorModules = creatorModules;
	}
	
	public Map<Integer, RestHanlderParam> getCode_paramMap() {
		return code_paramMap;
	}

	public void setCode_paramMap(Map<Integer, RestHanlderParam> code_paramMap) {
		this.code_paramMap = code_paramMap;
	}

	public Map<Integer, RespHanlderParam> getResp_map() {
		return resp_map;
	}

	public void setResp_map(Map<Integer, RespHanlderParam> resp_map) {
		this.resp_map = resp_map;
	}

	public void save() {
		String firstPath = System.getProperty("user.dir")+"\\src\\main\\resources";
//		logger.info("handler path:{}",firstPath);
		try {
            Path path = Paths.get(firstPath, fileDBName);
            File file = path.toFile();
            if(file.exists()) {
            	
            }
            Files.write(path,JSON.toJSONString(this).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("save handler!",e);
        }
	}
	
	public boolean loadDBFile() {
		Path path = Paths.get(ConstMockUI.resourcesPath, fileDBName);
        File file = path.toFile();
        if(file.exists()) {
        	try {
				byte[] readAllBytes = Files.readAllBytes(path);
				MockHandlerAnalysis ana = JSON.parseObject(new String(readAllBytes,StandardCharsets.UTF_8), MockHandlerAnalysis.class);
				setCreatorModules(ana.getCreatorModules());
				setCode_paramMap(ana.getCode_paramMap());
				setResp_map(ana.getResp_map());
				setAccount(ana.getAccount());
				return !ana.getCreatorModules().isEmpty();
			} catch (IOException e) {
				logger.error("read handler!",e);
			}
        }
        return false;
	}
	
	public static class RespHanlderParam{
		private int cmd;
		private String respSimpleName;
		private String respClassName;
		@JSONField(serialize = false)
		private RespResultAction action;
		@JSONField(serialize = false)
		private Class<?> clzFrom;
		public RespHanlderParam() {
		}
		public RespHanlderParam(int cmd, String respSimpleName, String respClassName) {
			this.cmd = cmd;
			this.respSimpleName = respSimpleName;
			this.respClassName = respClassName;
		}
		public RespHanlderParam(int cmd, String respSimpleName, String respClassName, RespResultAction action) {
			this.cmd = cmd;
			this.respSimpleName = respSimpleName;
			setRespClassName(respClassName);
			this.action = action;
		}
		public int getCmd() {
			return cmd;
		}
		public void setCmd(int cmd) {
			this.cmd = cmd;
		}
		public String getRespSimpleName() {
			return respSimpleName;
		}
		public void setRespSimpleName(String respSimpleName) {
			this.respSimpleName = respSimpleName;
		}
		public String getRespClassName() {
			return respClassName;
		}
		public void setRespClassName(String respClassName) {
			this.respClassName = respClassName;
			try {
				if(StringUtil.isNotEmpty(respClassName)) {
					this.clzFrom = Class.forName(respClassName);
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				logger.error("",e);
			}
		}
		public RespResultAction getAction() {
			return action;
		}
		public void setAction(RespResultAction action) {
			this.action = action;
		}
	}
	
	public static class RestHanlderParam{
		private String rqstMethodName;
		private int cmd;
		private String rqstClassName;
		@JSONField(serialize = false)
		private Class<?> clzFrom;
		public RestHanlderParam() {
		}
		public RestHanlderParam(String rqstMethodName, int cmd, String rqstClassName) {
			this.rqstMethodName = rqstMethodName;
			this.cmd = cmd;
			setRqstClassName(rqstClassName);
		}
		public String getRqstMethodName() {
			return rqstMethodName;
		}
		public void setRqstMethodName(String rqstMethodName) {
			this.rqstMethodName = rqstMethodName;
		}
		public int getCmd() {
			return cmd;
		}
		public void setCmd(int cmd) {
			this.cmd = cmd;
		}
		public String getRqstClassName() {
			return rqstClassName;
		}
		public void setRqstClassName(String rqstClassName) {
			this.rqstClassName = rqstClassName;
			try {
				if(StringUtil.isNotEmpty(rqstClassName)) {
					this.clzFrom = Class.forName(rqstClassName);
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				logger.error("",e);
			}
		}
		public Class<?> getClzFrom() {
			return clzFrom;
		}
	}

	public MockAccount getAccount() {
		return account;
	}
	public void setAccount(MockAccount account) {
		this.account = account;
	}
	public BlendedData getBlendedData() {
		return blendedData;
	}
	public void setBlendedData(BlendedData blendedData) {
		this.blendedData = blendedData;
	}
}
