package org.protobj.mock.javafx;

import com.alibaba.fastjson.JSON;
import com.guangyu.cd003.projects.common.cons.Commands;
import com.guangyu.cd003.projects.common.msg.RespFully;
import com.guangyu.cd003.projects.common.msg.RespFullyLst;
import com.guangyu.cd003.projects.common.msg.RespRawDatalizable;
import com.guangyu.cd003.projects.gs.module.common.msg.RespParamSet;
import com.guangyu.cd003.projects.gs.module.role.msg.CodeRole;
import com.guangyu.cd003.projects.gs.module.role.msg.RqstCreRole;
import com.guangyu.cd003.projects.gs.module.role.msg.RqstLoadRole;
import com.guangyu.cd003.projects.mock.IMockContext;
import com.guangyu.cd003.projects.mock.MockNettyGwFrtndTmnlCommHandler;
import com.pv.common.utilities.cons.ModuleID;
import com.pv.common.utilities.exception.ICode;
import com.pv.common.utilities.reflc.ReflcUtil;
import com.pv.terminal.netty.NettyGwFrtndTmnlTcpClient;
import com.pv.terminal.netty.NettyGwTmnl;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.DefaultChannelPromise;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class JavaFxMockContext extends NettyGwTmnl implements IMockContext {

    public Map<Integer, Class<Enum>> codeMap = new HashMap<>();

    NettyGwFrtndTmnlTcpClient nettyGwFrtndTmnlTcpClient;
    public static MockNettyGwFrtndTmnlCommHandler mockNettyGwFrtndTmnlCommHandler;
    JavaFxConnect javaFxConnect;

    public JavaFxMockContext() {
        mockNettyGwFrtndTmnlCommHandler = new MockNettyGwFrtndTmnlCommHandler(this);
        Set<Class<? extends ICode>> classes = ReflcUtil.forClassSubTypeOf("com.guangyu.cd003.projects", ICode.class);
        for (Class<? extends ICode> aClass : classes) {
            Class enumClass = aClass;
            if (enumClass.getEnumConstants().length == 0) {
                continue;
            }
            ICode enumConstant = (ICode) enumClass.getEnumConstants()[0];
            ModuleID moduleID = enumConstant.getModuleID();
            int id = (moduleID.getValue());
            codeMap.put(id, enumClass);
        }
        String id = "client_1001";
        String name = "client_1001";
        String group = "client";
        try {
            this.nettyGwFrtndTmnlTcpClient = new NettyGwFrtndTmnlTcpClient(id, name, group, null, decoderSupplier, encoderSupplier, mockNettyGwFrtndTmnlCommHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void login(String account, String gateway, String serverId) {
        javaFxConnect = new JavaFxConnect();
        javaFxConnect.serverId = serverId;
        javaFxConnect.account = account;
        String[] split = gateway.split(":");
        nettyGwFrtndTmnlTcpClient.startupWithBindedDeal(split[0], Integer.parseInt(split[1]), (cli, f) -> {
            if (f instanceof ChannelFuture) {
                if (((ChannelFuture) f).isSuccess()) {
                    ChannelFuture channelFuture = (ChannelFuture) f;
                    Channel channel = channelFuture.channel();
                    javaFxConnect.channel = channel;
                    channel.attr(AttributeKey.valueOf(MockNettyGwFrtndTmnlCommHandler.ATTR_NAME_SERVER_ID)).set("gs_" + serverId);
                    channel.attr(AttributeKey.valueOf(MockNettyGwFrtndTmnlCommHandler.ATTR_NAME_REGULAR_PARAM)).set(account);
                    LogUI.log("connect success");
                } else {
                    LogUI.log("connect error");
                }
            }
        });
    }

    @Override
    public void onRegularSucc(Channel channel) {
        LogUI.log("regular success");
        RqstLoadRole sendBytes = new RqstLoadRole();
        sendBytes.sid = javaFxConnect.serverId;
        javaFxConnect.send(101, sendBytes);
    }

    @Override
    public void onEvent(Channel channel, Integer cmd, Object result) {

        //收到消息包
        if (result.equals(Commands.COMMON_HEARBEAT_CONST)) {
            return;
        }
        if (result instanceof RespFullyLst) {
            for (RespFully resp : ((RespFullyLst) result).resps) {
                for (Map.Entry<Integer, RespRawDatalizable> integerRespRawDatalizableEntry : resp.opAndInfos.entrySet()) {
                    RespRawDatalizable msg = integerRespRawDatalizableEntry.getValue();
                    if (msg instanceof RespParamSet) {

                    }
                    LogUI.log("mock recv mainCmd0:{} typeSign:{}->{}", cmd, msg.typeSignOfRawData(), JavaFxMockApplication.gson.toJson(msg));
                }
                if (resp.sysTime != null && resp.sysTime.value > 0) {
                    LogUI.log("recv sysTime:{}", new Date(resp.sysTime.value).toLocaleString());
                }
            }
        } else {
            //单消息处理
            if (result instanceof Integer && ((Integer) result) != 0) {
                Integer paramA1 = (Integer) result;
                Class<Enum> enumClass = codeMap.get(paramA1 / 100);
                if (enumClass != null) {
                    Enum[] enumConstants = enumClass.getEnumConstants();
                    Enum enumConstant = enumConstants[paramA1 % 100 - 1];
                    LogUI.log("mock recv mainCmd1:{}->{}", cmd, enumConstant);
                } else {
                    LogUI.log("mock recv mainCmd2:{}->{}", cmd, JavaFxMockApplication.gson.toJson(result));
                }
            } else {
                LogUI.log("mock recv mainCmd3:{}->{}", cmd, JavaFxMockApplication.gson.toJson(result));
            }
        }

        if (cmd == 101 && result.equals(CodeRole.CANNT_FIND_ROLE.getCode())) {
            //没有账号
            RqstCreRole rqstCreRole = new RqstCreRole();
            rqstCreRole.camp = 1;
            rqstCreRole.country = 1;
            rqstCreRole.sid = javaFxConnect.serverId;
            rqstCreRole.name = javaFxConnect.account;
            javaFxConnect.send(Commands.ROLE_CRE_CONST, rqstCreRole);
        }
    }

    @Override
    public void removeConnect(Channel channel) {
        javaFxConnect = null;
    }


    public static class JavaFxConnect {
        public String account;
        public Channel channel;
        public String serverId;
        public void send(Integer cmd) {
            JavaFxMockContext.mockNettyGwFrtndTmnlCommHandler.req(channel, cmd);
            if (cmd != Commands.COMMON_HEARBEAT_CONST) {
                LogUI.log("mock send:{}", cmd);
            }
        }

        public void send(Integer cmd, Object sendBytes) {
            LogUI.log("mock send:{}->{}", cmd, JSON.toJSONString(sendBytes));
            JavaFxMockContext.mockNettyGwFrtndTmnlCommHandler.req(channel, cmd, sendBytes);
        }


        public void close() {
            DefaultChannelPromise promise = new DefaultChannelPromise(channel);

            channel.close(promise).addListener(new GenericFutureListener<Future<? super Void>>() {
                @Override
                public void operationComplete(Future<? super Void> future) throws Exception {
                    LogUI.log("logout success");
                }
            });
        }
    }
}
