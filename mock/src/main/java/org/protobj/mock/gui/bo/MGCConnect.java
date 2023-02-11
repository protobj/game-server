package org.protobj.mock.gui.bo;

import java.util.HashMap;
import java.util.Map;

import com.guangyu.cd003.projects.mock.ui.support.TextShowSupport;
import com.guangyu.cd003.projects.mock.ui.support.TextShowUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.guangyu.cd003.projects.common.cons.Commands;
import com.guangyu.cd003.projects.mock.MockHandlerAnalysis;
import com.guangyu.cd003.projects.mock.MockHandlerAnalysis.RestHanlderParam;
import com.guangyu.cd003.projects.mock.gui.netty.MGCNettyGwFrtndTmnlHandler;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.pv.terminal.netty.NettyGwTmnl;

import io.netty.channel.Channel;
import io.netty.channel.DefaultChannelPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

public class MGCConnect extends NettyGwTmnl {
    Logger logger = getLogger();
    private String uid;
    private String serverId;
    private Channel channel;
    public MGCContext MGCContext;
    public Map<Integer, Long> sendTimes = new HashMap<>();
    public long lastSendTime = 0;
    public MockHandlerAnalysis handlerAnalysis;
    private MGCNettyGwFrtndTmnlHandler mgcNettyHandler;
    public MGCConnect(MGCContext context, String uid, String serverId, MockHandlerAnalysis handlerAnalysis) {
    	this.MGCContext = context;
        this.uid = uid;
        this.serverId = serverId;
        this.handlerAnalysis = handlerAnalysis;
        this.mgcNettyHandler = context.getHandler();
	}

    public String getUid() {
        return uid;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public Channel getChannel() {
        return channel;
    }

    public void send(Object[] cmd, Object sendBytes) {
        int intCmd = (int) cmd[1];
        sendTimes.put(intCmd, System.nanoTime());
        lastSendTime = System.currentTimeMillis();
        logger.info("mock send:{}->{}", cmd[0], JSON.toJSONString(sendBytes));
        mgcNettyHandler.req(channel, intCmd, sendBytes);
    }

    public void send(Integer cmd, Object sendBytes) {
        RestHanlderParam codeParamBy = this.handlerAnalysis.getCodeParamBy(cmd);
        if (codeParamBy == null) {
            logger.error("not find cmd:{} handler!!!", cmd);
            return;
        }
        sendTimes.put(codeParamBy.getCmd(), System.nanoTime());
        lastSendTime = System.currentTimeMillis();
        logger.info("mock send:{}->{}", codeParamBy.getCmd(), JSON.toJSONString(sendBytes));
        mgcNettyHandler.req(channel, cmd, sendBytes);
    }


    public void send(Object[] cmd) {
        int intCmd = (int) cmd[1];
        sendTimes.put(intCmd, System.nanoTime());
        lastSendTime = System.currentTimeMillis();
        mgcNettyHandler.req(channel,intCmd);
        if (intCmd != Commands.COMMON_HEARBEAT_CONST) {
            logger.info("mock send:{}", cmd[0]);
        }
    }

    public void send(Integer cmd) {
        RestHanlderParam codeParamBy = this.handlerAnalysis.getCodeParamBy(cmd);
        if (codeParamBy == null) {
            logger.error("not find cmd:{} handler!!!", cmd);
            return;
        }
        int intCmd = codeParamBy.getCmd();
        sendTimes.put(intCmd, System.nanoTime());
        lastSendTime = System.currentTimeMillis();
        mgcNettyHandler.req(channel,intCmd);
        if (intCmd != Commands.COMMON_HEARBEAT_CONST) {
            logger.info("mock send:{}", intCmd);
        }
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
    
    
    public void close() {
      DefaultChannelPromise promise = new DefaultChannelPromise(channel);
      channel.close(promise).addListener(new GenericFutureListener<Future<? super Void>>() {
          @Override
          public void operationComplete(Future<? super Void> future) throws Exception {
              logger.info("login out!!");
          }
      });
    }
    
    public Logger getLogger() {
    	if(this.logger == null) {
    		this.logger = TextShowUtil.creLogger(MGCConnect.class);
    	}
    	return this.logger;
    }
	
	
	

}
