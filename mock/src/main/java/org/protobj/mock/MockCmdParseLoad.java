package org.protobj.mock;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.guangyu.cd003.projects.mock.common.ConstMockUI;
import com.guangyu.cd003.projects.mock.common.MockGameCmd;
import com.guangyu.cd003.projects.mock.ui.support.TextShowUtil;

public class MockCmdParseLoad {
	static final Logger logger = TextShowUtil.creLogger(MockCmdParseLoad.class);
	static final String fileDBName = "cmd.cfg";
	private List<MockGameCmd> cmds = new ArrayList<>();
	
	public void loadCmd() {
		Path path = getCmdCfgPath();
        File file = path.toFile();
        if(file.exists()) {
        	cmds.clear();
        	try {
        		List<String> readAllLines = Files.readAllLines(path, StandardCharsets.UTF_8);
        		readAllLines.forEach((lineStr)->{
        			lineStr = lineStr.trim();
        			if(StringUtils.isNotEmpty(lineStr) && lineStr.indexOf("//") == -1) {
//	        			logger.info("load cmd:{}",lineStr);
	        			MockGameCmd parseObject = JSON.parseObject(lineStr, MockGameCmd.class);
	        			if(parseObject.getCmd() > 0) {
	        				cmds.add(parseObject);
	        			}else {
	        				logger.error("cmd is empty!->{}",parseObject.getCmd());
	        			}
        			}
        		});
			} catch (IOException e) {
				logger.error("read cmd!",e);
			}
        }else {
        	try {
				 Files.write(path, JSON.toJSONString(new MockGameCmd(), SerializerFeature.WriteNullStringAsEmpty).getBytes(StandardCharsets.UTF_8));
			} catch (IOException e) {
				logger.error("read cmd!",e);
			}
        }
	}
	
	public List<MockGameCmd> forCmds(){
		return this.cmds;
	}
	
	public Path getCmdCfgPath() {
        return Paths.get(ConstMockUI.resourcesPath, fileDBName);
	}
}
