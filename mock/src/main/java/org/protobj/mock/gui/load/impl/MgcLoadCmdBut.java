package org.protobj.mock.gui.load.impl;

import org.slf4j.Logger;

import com.guangyu.cd003.projects.mock.MockCmdParseLoad;
import com.guangyu.cd003.projects.mock.MockHandlerAnalysis;
import com.guangyu.cd003.projects.mock.MockHandlerAnalysis.RestHanlderParam;
import com.guangyu.cd003.projects.mock.gui.bo.MGCConnect;
import com.guangyu.cd003.projects.mock.gui.bo.MGCContext;
import com.guangyu.cd003.projects.mock.gui.bo.MGCLobby;
import com.guangyu.cd003.projects.mock.gui.load.IMGCIniLoad;
import com.guangyu.cd003.projects.mock.ui.MockButton;
import com.guangyu.cd003.projects.mock.ui.MockUIContext;
import com.guangyu.cd003.projects.mock.ui.support.TextShowUtil;
import com.pv.common.utilities.common.StringUtil;

public class MgcLoadCmdBut implements IMGCIniLoad {
	static final Logger logger = TextShowUtil.creLogger(MgcLoadCmdBut.class);
	@Override
	public boolean initLoad(MGCLobby lobby) {
		MockUIContext context = lobby.context;
		MockCmdParseLoad cmdParseLoad = lobby.cmdParseLoad;
		MockHandlerAnalysis analysis = lobby.analysis;
		MGCContext mockContext = lobby.mockContext;
		  cmdParseLoad.forCmds().forEach((gameCmd) -> {
	            MockButton creBtn = new MockButton(gameCmd.getCmdName(), (btn) -> {
	            	MGCConnect mockConnect = mockContext.getConnectMap().get(analysis.getAccount().getUid());
	                if (mockConnect != null) {
	                    if (StringUtil.isEmpty(gameCmd.getMsg())) {
	                        mockConnect.send(gameCmd.getCmd());
	                    } else {
	                        RestHanlderParam codeParamBy = analysis.getCodeParamBy(gameCmd.getCmd());
	                        if (codeParamBy != null && codeParamBy.getClzFrom() != null) {
	                            mockConnect.send(gameCmd.getCmd(), gameCmd.getMsgBy(codeParamBy.getClzFrom()));
	                        } else {
	                            logger.info("not find rqstHandler：{}-{}", gameCmd.getCmd(), gameCmd.getMsg());
	                        }
	                    }
	                } else {
	                    logger.info("用户：{} 链接已断开", analysis.getAccount().getUid());
	                }
	            });
	            int lineNum = context.countButs();
	            int y = creBtn.getHeight() * (lineNum / 3);
	            int x = lineNum % 3 == 0 ? 0 : creBtn.getWidth() * (lineNum % 3);
	            creBtn.setLocation(x, y);
	            context.addBut(creBtn);
	        });
		return true;
	}

	@Override
	public int loadType() {
		return IMGCIniLoad.Load_CmdBtn;
	}

}
