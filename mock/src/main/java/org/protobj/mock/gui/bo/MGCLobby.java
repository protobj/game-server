package org.protobj.mock.gui.bo;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;

import com.guangyu.cd003.projects.mock.MockCmdParseLoad;
import com.guangyu.cd003.projects.mock.MockHandlerAnalysis;
import com.guangyu.cd003.projects.mock.common.ConstMockUI;
import com.guangyu.cd003.projects.mock.common.MockAccount;
import com.guangyu.cd003.projects.mock.gui.load.IMGCIniLoad;
import com.guangyu.cd003.projects.mock.gui.load.impl.MgcLoadAction;
import com.guangyu.cd003.projects.mock.gui.load.impl.MgcLoadCmdBut;
import com.guangyu.cd003.projects.mock.gui.load.impl.MgcLoadMenu;
import com.guangyu.cd003.projects.mock.ui.MockUIContext;
import com.guangyu.cd003.projects.mock.ui.support.TextShowUtil;
import com.pv.common.utilities.common.CommonUtil;

/**
 * 客户端
 *
 * @author ChiangHo
 */
public class MGCLobby{
    static final Logger logger = TextShowUtil.creLogger(MGCLobby.class);
    public ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
    public MGCContext mockContext;
    public MockUIContext context;
    public MockHandlerAnalysis analysis;
    public MockCmdParseLoad cmdParseLoad;
    public List<IMGCIniLoad> loads;
    public Map<Integer, IMGCIniLoad> loadMaps;
    public void start() {
        context = new MockUIContext();
        context.startUI();
        if (intSystem()) {
            try {
            	loads.forEach((mgcLoad)->mgcLoad.initLoad(this));
            } catch (Exception e) {
                logger.error("", e);
            }
        }
    }

    public void loadList() {
    	addInitLoad(new MgcLoadAction());	
    	addInitLoad(new MgcLoadCmdBut());	
    	addInitLoad(new MgcLoadMenu());	
    }
    
    public boolean intSystem() {
        try {
            loadList();
            analysis = new MockHandlerAnalysis();
            analysis.loadDBFile();
            MockAccount account = analysis.getAccount();
            cmdParseLoad = new MockCmdParseLoad();
            cmdParseLoad.loadCmd();
            logger.info("filePath:{}", ConstMockUI.resourcesPath);
            initLoad(IMGCIniLoad.Load_Action);
            logger.info("服务器ID：{}", analysis.getAccount().getServerId());
            System.setProperty("cq.mock", "true");
            mockContext = new MGCContext();
            logger.info("initHandler ps：比较消耗时间，耐心等候");
            long sect = System.currentTimeMillis();
            MGCLobby.this.mockContext.initHandler();
            logger.info("initHandler end->{}ms",(System.currentTimeMillis() - sect));
            logger.info("开始初始化Net");
            MGCLobby.this.mockContext.initNet(account.getUid(),account.getUid(),"client");
            logger.info("开始初始化Net complete!!");
            logger.info("=========可以执行登录操作了============");
        } catch (Exception e) {
            logger.error("", e);
            return false;
        }
        return true;
    }
    
    
    public void addInitLoad(IMGCIniLoad load) {
    	if(loads == null) {
    		loads = CommonUtil.createList();
    		loadMaps = CommonUtil.createMap();
    	}
    	if(!loadMaps.containsKey(load.loadType())) {
    		loadMaps.put(load.loadType(), load);
    		loads.add(load);
    	}
    }
    
    public boolean initLoad(int loadType) {
    	IMGCIniLoad imgcIniLoad = loadMaps.get(loadType);
    	if(imgcIniLoad != null) {
    		return imgcIniLoad.initLoad(this);
    	}
    	return false;
    }
}
