package org.protobj.mock.gui.load.impl;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

import com.guangyu.cd003.projects.gs.module.role.msg.RqstCreRole;
import com.guangyu.cd003.projects.gs.module.role.msg.RqstLoadRole;
import com.guangyu.cd003.projects.mock.MockHandlerAnalysis;
import com.guangyu.cd003.projects.mock.gui.bo.MGCConnect;
import com.guangyu.cd003.projects.mock.gui.bo.MGCContext;
import com.guangyu.cd003.projects.mock.gui.bo.MGCLobby;
import com.guangyu.cd003.projects.mock.gui.load.IMGCIniLoad;
import com.guangyu.cd003.projects.mock.ui.support.TextShowUtil;

public class MgcLoadAction implements IMGCIniLoad {
	static final Logger logger = TextShowUtil.creLogger(MgcLoadAction.class);
	@Override
	public boolean initLoad(MGCLobby lobby) {
		MockHandlerAnalysis analysis = lobby.analysis;
		analysis.regstRespAction(MGCContext.CMD_ConectionGateWaySuc, (connect,handlerResult, paramA,code)->{
			loadRole(analysis,connect);
		});
        analysis.regstRespAction(101, (connect, handlerResult, paramA, code) -> {
            if (code != 0) {
                RqstCreRole msg = new RqstCreRole();
                msg.sid = analysis.getAccount().getServerId();
                msg.country = 0;
                msg.camp = 0;
                msg.name = connect.getUid();
                connect.send(102, msg);
                logger.info("===注册玩家角色：{}==", code);
            } else {
                logger.info("===玩家登录成功==");
                connect.send(9901);
            }
        });
        analysis.regstRespAction(9901, (connect, handlerResult, paramA, code) -> {
//			logger.info("{}===收到心跳回执！",connect.getUid());
        	lobby.scheduledExecutorService.schedule(() -> {
                connect.send(9901);
            }, 30, TimeUnit.SECONDS);
        });
		return true;
	}

	@Override
	public int loadType() {
		return IMGCIniLoad.Load_Action;
	}
	
	
  public void loadRole(MockHandlerAnalysis analysis,MGCConnect mockConnect) {
        logger.info("开始loadRole->{}:{}", analysis.getAccount().getUid(), analysis.getAccount().getServerId());
        RqstLoadRole msg = new RqstLoadRole();
        msg.sid = analysis.getAccount().getServerId();
        mockConnect.send(101, msg);
    }
}
