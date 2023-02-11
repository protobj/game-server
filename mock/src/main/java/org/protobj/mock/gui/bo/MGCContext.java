package org.protobj.mock.gui.bo;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

import org.slf4j.Logger;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import com.guangyu.cd003.projects.common.cons.Commands;
import com.guangyu.cd003.projects.common.msg.RespFully;
import com.guangyu.cd003.projects.common.msg.RespFullyLst;
import com.guangyu.cd003.projects.common.msg.RespRawData;
import com.guangyu.cd003.projects.common.msg.RespRawDatalizable;
import com.guangyu.cd003.projects.mock.HandlerResult;
import com.guangyu.cd003.projects.mock.IMockContext;
import com.guangyu.cd003.projects.mock.MockNettyGwFrtndTmnlCommHandler;
import com.guangyu.cd003.projects.mock.MockRecord;
import com.guangyu.cd003.projects.mock.common.RespResultAction;
import com.guangyu.cd003.projects.mock.gui.netty.MGCNettyGwFrtndTmnlHandler;
import com.guangyu.cd003.projects.mock.ui.support.TextShowUtil;
import com.pv.common.utilities.common.GsonUtil;
import com.pv.framework.gs.core.module.msgproc.NullRqstMsg;
import com.pv.terminal.netty.NettyGwFrtndTmnlTcpClient;
import com.pv.terminal.netty.NettyGwTmnl;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

/**
 * Created on 2021/5/20.
 *
 * @author chen qiang
 */
public class MGCContext extends NettyGwTmnl implements IMockContext {
	 Logger logger = getLogger();
	public static final int CMD_ConectionGateWaySuc = -1;
	
	public static AttributeKey<MGCConnect> mockAttributeKey = AttributeKey.newInstance("mockConnect");
	
	public NettyGwFrtndTmnlTcpClient nettyGwFrtndTmnlTcpClient;
    public MGCNettyGwFrtndTmnlHandler mgcNettyHandler;
    public ScheduledExecutorService eventExecutors;
    public Map<String, MGCConnect> connectMap = Maps.newConcurrentMap();
    public MockRecord mockRecord = new MockRecord();
   
    public Map<String, RespResultAction> respResultActionMap = new HashMap<>();

    public void initHandler() throws Exception {
    	if(mgcNettyHandler == null)
    		mgcNettyHandler = new MGCNettyGwFrtndTmnlHandler(this);
    	if(eventExecutors == null)
    		eventExecutors = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
    }
    
    public MGCNettyGwFrtndTmnlHandler getHandler() {
    	return this.mgcNettyHandler;
    }
    
    public void regstRespAction(String key, RespResultAction action) {
        respResultActionMap.put(key, action);
    }
    
    public void initNet(String id,String name,String group)throws InstantiationException, IllegalAccessException {
         nettyGwFrtndTmnlTcpClient = new NettyGwFrtndTmnlTcpClient(group+"_"+id, group+"_"+name, group, null, decoderSupplier, encoderSupplier, mgcNettyHandler);
    }

    private Consumer<MGCConnect> disConnect;

    public void listenDisConnection(Consumer<MGCConnect> disConnect) {
        this.disConnect = disConnect;
    }
    
    //
  public final void onEvent(Channel channel, Integer cmd, Object result) {
	  MGCConnect connect = (MGCConnect) channel.attr(mockAttributeKey).get();
      HandlerResult handlerResult = new HandlerResult();
      if (result instanceof RespFullyLst) {
    	  RespFullyLst lstmsg = ((RespFullyLst) result);
        	if(lstmsg.resps.isEmpty()) {
        		if (cmd != Commands.COMMON_HEARBEAT_CONST)logger.info("mgc recv({}) msg empty!!!",cmd);
        		RespResultAction respResultAction = connect.handlerAnalysis.getRespAction(cmd);
        		if(respResultAction != null) {
                	respResultAction.action(connect, handlerResult, null, 0);
                }
        	}else {
        		for (RespFully resp : lstmsg.resps) {
                    for (Map.Entry<Integer, RespRawDatalizable> integerRespRawDatalizableEntry : resp.opAndInfos.entrySet()) {
                        RespRawDatalizable msg = integerRespRawDatalizableEntry.getValue();
                        logger.info("mgc recv({}) typeSign:{}->{}", cmd, integerRespRawDatalizableEntry.getKey(), GsonUtil.toJSONString(msg));
//                        String name = msg.getClass().getSimpleName();
                        RespResultAction respResultAction = connect.handlerAnalysis.getRespAction(integerRespRawDatalizableEntry.getKey());
                        if(respResultAction != null) {
                        	respResultAction.action(connect, handlerResult, msg, 0);
                        }
//                        else {
//                        	logger.info("cmd:{} not find respAction!",cmd);
//                        }
                    }
                    if (resp.sysTime != null && resp.sysTime.value > 0) {
                        logger.info("recv sysTime:{}", resp.sysTime.value);
                    }
                }
        	}
      } else {
          //单消息处理
          if (result instanceof Integer && ((Integer) result) != 0) {
//              Integer paramA1 = (Integer) result;
              logger.info("mgc recv({}) ->errorCode:{}", cmd, JSON.toJSONString(result));
//              Class<Enum> enumClass = codeMap.get(paramA1 / 100);
//              if (enumClass != null) {
//                  Enum[] enumConstants = enumClass.getEnumConstants();
//                  Enum enumConstant = enumConstants[paramA1 % 100 - 1];
//                  logger.info("mock1 recv({}) ->{}", cmd, enumConstant);
//              } else {
//                  logger.info("mock2 recv({}) ->errorCode:{}", cmd, JSON.toJSONString(result));
//              }
          } else {
              logger.info("mgc recv({}) ->{}", cmd, JSON.toJSONString(result));
          }
          Class<?> classByTypeSign = RespRawData.getClassByTypeSign(cmd);
          if (classByTypeSign != null) {
//              String simpleName = classByTypeSign.getSimpleName();
              RespResultAction respResultAction = connect.handlerAnalysis.getRespAction(cmd);
              if(respResultAction != null) {
              	 if (result instanceof Integer) {
                       respResultAction.action(connect, handlerResult, null, (int)result);
                   } else if (result instanceof NullRqstMsg) {
                  	 respResultAction.action(connect, handlerResult, null, 0);
                   } else {
                  	 respResultAction.action(connect, handlerResult, result, 0);
                   }
              }
//              else {
//              	logger.info("cmd:{} not find respAction!",cmd);
//              }
          } else {
              logger.warn("mgc消息 {} 没有处理", cmd);
          }
      }
  }

  
  public void connect(MGCConnect mockConnect, String url, Consumer<Integer> listen) {
      String[] split = url.split(":");
      nettyGwFrtndTmnlTcpClient.startupWithBindedDeal(split[0], Integer.parseInt(split[1]), (cli, f) -> {
          if (f instanceof ChannelFuture) {
              if (((ChannelFuture) f).isSuccess()) {
                  ChannelFuture channelFuture = (ChannelFuture) f;
                  Channel channel = channelFuture.channel();
                  mockConnect.setChannel(channel);
                  channel.attr(mockAttributeKey).set(mockConnect);
                  logger.error("连接成功->{}", url);
                  channel.attr(AttributeKey.valueOf(MockNettyGwFrtndTmnlCommHandler.ATTR_NAME_SERVER_ID)).set("gs_"+mockConnect.getServerId());
                  channel.attr(AttributeKey.valueOf(MockNettyGwFrtndTmnlCommHandler.ATTR_NAME_REGULAR_PARAM)).set(mockConnect.getUid());
                  connectMap.put(mockConnect.getUid(), mockConnect);
                  if (listen != null) listen.accept(0);
              } else {
                  logger.error("连接失败->{}", mockConnect.getUid());
                  if (listen != null) listen.accept(1);
              }
          }
      });
  }
  
  public Map<String, MGCConnect> getConnectMap() {
      return connectMap;
  }

   @Override
   public void removeConnect(Channel channel) {
        Attribute<MGCConnect> attr = channel.attr(mockAttributeKey);
        MGCConnect mockConnect = attr.getAndSet(null);
        if (mockConnect != null) {
            logger.error("连接断开 :{}", mockConnect.getUid());
            connectMap.remove(mockConnect.getUid());
            if (disConnect != null) disConnect.accept(mockConnect);//else logger.info("not find disConnect");
        }
    }
    
    
    @Override
    public void onRegularSucc(Channel channel) {
    	MGCConnect mockConnect = channel.attr(mockAttributeKey).get();
        RespResultAction respResultAction = mockConnect.handlerAnalysis.getRespAction(CMD_ConectionGateWaySuc);
        if(respResultAction!= null) {
        	respResultAction.action(mockConnect, null, null, CMD_ConectionGateWaySuc);
        }
    }
    
    public Logger getLogger() {
    	if(this.logger == null) {
    		this.logger = TextShowUtil.creLogger(MGCContext.class);
    	}
    	return this.logger;
    }
}
