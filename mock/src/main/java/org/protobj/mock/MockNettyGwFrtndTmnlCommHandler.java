package org.protobj.mock;

import com.baidu.bjf.remoting.protobuf.Codec;
import com.baidu.bjf.remoting.protobuf.ProtobufProxy;
import com.guangyu.cd003.projects.common.msg.ProjectSGSJProtobufCodec;
import com.guangyu.cd003.projects.common.msg.RespFully;
import com.guangyu.cd003.projects.common.msg.RespFullyLst;
import com.guangyu.cd003.projects.gs.ProjectSGsApplication;
import com.pv.common.net.core.BaseMsg;
import com.pv.common.net.core.BaseMsgTypes;
import com.pv.common.net.core.BaseMsgTypes.ExtType;
import com.pv.common.net.core.BaseMsgTypes.Type;
import com.pv.common.net.core.Connc;
import com.pv.common.net.core.MultiConncSession;
import com.pv.common.utilities.compress.SnappyUtil;
import com.pv.terminal.core.GwBkndTmnlCommHandler;
import com.pv.terminal.netty.NettyGwFrtndTmnlCommHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class MockNettyGwFrtndTmnlCommHandler extends NettyGwFrtndTmnlCommHandler {
    public static final ExtType CTRL_EXT_TYPE_REGULARIZE = ExtType.CTRL_CUSTOM_EXT_TYPE_0;
    public static final ExtType CTRL_EXT_TYPE_RECONNC = ExtType.CTRL_CUSTOM_EXT_TYPE_1;
    public static final String ATTR_NAME_SECRET = "scrt";
    public static final String ATTR_NAME_GATEWAY = "gw";
    public static final String ATTR_NAME_SERVER_ID = "server_id";
    private static final Object ATTR_NAME_SESSION = "session";
    public static final String ATTR_NAME_REGULAR_PARAM = "ATTR_NAME_REGULAR_PARAM";
    ProjectSGSJProtobufCodec projectSGSJProtobufCodec = new ProjectSGSJProtobufCodec();

    private IMockContext mockContext;

    volatile Logger logger = getLogger();

    private static final class DemoFrtndTmnlSession extends MultiConncSession {
        private Logger logger;

        public DemoFrtndTmnlSession(Object agentTgtId, Logger logger) {
            super("frtndTmnl", "frtndTmnlGrp", agentTgtId);
            this.logger = logger;
        }

        @SuppressWarnings("rawtypes")
        @Override
        protected void afterRmConnc(Connc connc) {
          //  logger.info("connc[{}] close from session[{}], current conncs:[{}]", connc.getUuid(), getAgentTgtId(), getConncs().size());
        }

    }


    public MockNettyGwFrtndTmnlCommHandler(IMockContext mockContext) {
        super(false, false);
        this.mockContext = mockContext;
        this.projectSGSJProtobufCodec.init(ProjectSGsApplication.RQST_PACKAGES, ProjectSGsApplication.RESP_PACKAGES);
    }

    @Override
    public void onConnClose(ChannelHandlerContext ctx) {
        super.onConnClose(ctx);
        mockContext.removeConnect(ctx.channel());
    }

    @Override
    public void onConncOpen(ChannelHandlerContext ctx) {
        String serverId = getAttrFromCtx(ctx, ATTR_NAME_SERVER_ID);
        if (serverId == null) {
            if (mockContext instanceof MockContext) {
                serverId = ((MockContext) mockContext).config.getServerId();
            } else {
                throw new NullPointerException("serverId is null");
            }
        }
        sendMsg(ctx, createBaseMsgWithMiniHeadParams(Type.CTRL_CONNC_OPEN, ExtType.CTRL_CONNC_OPEN_EXT_TYPE_BASIC, serverId));
    }


    @Override
    public void onException(ChannelHandlerContext ctx, Throwable t) {
        ctx.close();
        t.printStackTrace();
    }

    @Override
    public void consumeHeartBeat(ChannelHandlerContext ctx, BaseMsg<ByteBuf> m) {
        m.release();
    }

    @Override
    public void customCtrl(ChannelHandlerContext ctx, BaseMsg<ByteBuf> msg) {
        ExtType extType = msg.getExtType();
        if (CTRL_EXT_TYPE_REGULARIZE == extType) {//合法化处理(合法化定义为允许进入BizMsg处理的一系列校验操作), 首先取得secret 以及 网关的frontendConncId
            /*
             * 合法化处理分为以下阶段:
             * a.GatewayBackendTerminal中提取gatewayId, fConncId, secret, 以及使用应用自定义的处理方法提取应用自身需要的合法化处理参数
             * b.将上述取得的参数提交给应用的合法化校验模块(异步)进行处理
             * c.应用处理完毕在回调GatewayBackendTerminal的合法化方法通知gateway
             */
            mockContext.onRegularSucc(ctx.channel());
        }
        msg.release();
    }

    @Override
    public void tgtendConncClose(ChannelHandlerContext ctx, BaseMsg<ByteBuf> m) {
        Connc<ChannelHandlerContext> connc = getAttrFromCtx(ctx, ATTR_NAME_CONNC);
        if (connc != null) {
            switch (m.getExtType()) {
                case CTRL_CONNC_CLOSE_EXT_TYPE_WITH_CAUSE_CODE:
                    Integer code = takeMiniDataHeadFrom(m);
                   // logger.info("connc[{}][{}] closed by tgt, caused by [{}]", connc.getUuid(), connc.getAttVal(ATTR_NAME_CONNC), code);
                    break;
                case CTRL_CONNC_CLOSE_EXT_TYPE_NO_PARAM:
               //     logger.info("connc closed without cause");
                    break;
                default:
                    break;
            }
        }
        m.release();
    }

    @Override
    public void tgtendConnc(ChannelHandlerContext ctx, BaseMsg<ByteBuf> m) {
        String serverId = getAttrFromCtx(ctx, ATTR_NAME_SERVER_ID);
        if (serverId == null) {
            if (mockContext instanceof MockContext) {
                serverId = ((MockContext) mockContext).config.getServerId();
            } else {
                throw new NullPointerException("serverId is null");
            }
        }
        Integer fConncId = takeMiniDataHeadFrom(m);
        String secret = takeMiniDataHeadFrom(m);
        String gwId = takeMiniDataHeadFrom(m);
        Connc<ChannelHandlerContext> connc = buildConnc(ctx, fConncId);
        connc.putAtt(ATTR_NAME_SECRET, secret);
        connc.putAtt(ATTR_NAME_GATEWAY, gwId);
        setAttrToCtx(ctx, ATTR_NAME_CONNC, connc);

        String session_id = gwId + serverId;

        MultiConncSession session = getOrCreSession(session_id, sid -> new DemoFrtndTmnlSession(sid, this.logger));
        session.addConnc(connc);
        connc.putAtt(ATTR_NAME_SESSION, session);
        Object attrFromCtx = getAttrFromCtx(ctx, ATTR_NAME_REGULAR_PARAM);
        //logger.warn("server[{}] through by gateway[{}]'s connc[{}][{}] estanblished regulerParam:{}", serverId, gwId, fConncId, secret, attrFromCtx);
        sendRegularMsg(gwId, serverId, fConncId, attrFromCtx.toString());
        m.release();
    }

    @Override
    public void onMsgRecv(ChannelHandlerContext ctx, BaseMsg<ByteBuf> m) {
        Logger logger = getLogger();
        if (m.getType() == Type.ERR_MSG) {
            switch (m.getExtType()) {
                case ERR_MSG_EXT_TYPE_CODE: {
                    Integer code = takeMiniDataHeadFrom(m);
                    logger.error("recv error msg code:{}", code);
                    break;
                }
                case ERR_MSG_EXT_TYPE_CODE_WITH_CAUSE: {
                    Integer code = takeMiniDataHeadFrom(m);
                    Integer cause = takeMiniDataHeadFrom(m);

                    mockContext.onEventBeforeDecode(ctx.channel(), cause, code);
                    mockContext.onEvent(ctx.channel(), cause, code);
                    break;
                }
                default:
                    break;
            }
            m.release();
        } else {
            super.onMsgRecv(ctx, m);
        }


    }

    public byte[] unCompress(int cmd,BaseMsg<ByteBuf> m){
        byte[] reultData = takeRemainBytes(m);
        if(m.getCompression() > 0) {
            try {
                reultData = SnappyUtil.decompress(reultData);
            } catch (IOException e) {
                logger.error("",e);
            }
        }
//        logger.info("cmd:{} compression:{} bytes:{}",cmd,m.getCompression(), Arrays.toString(reultData));
        return reultData;
    }

    @Override
    public void consumeBizMsg(ChannelHandlerContext ctx, BaseMsg<ByteBuf> m) {
        try {
            Integer proxyType = takeMiniDataHeadFrom(m);
            Integer cmd = takeMiniDataHeadFrom(m);
            mockContext.onEventBeforeDecode(ctx.channel(), cmd, 0);
            byte[] protoBytes = unCompress(cmd.intValue(),m);
            try {
                Class<RespFullyLst> respFullyLstClass = RespFullyLst.class;
                Codec<RespFullyLst> respFullyLstCodec = ProtobufProxy.create(respFullyLstClass);
                RespFullyLst decode = respFullyLstCodec.decode(protoBytes);
                for (RespFully resp : decode.resps) {
                    resp.decode();
                }
                mockContext.onEvent(ctx.channel(), cmd, decode);
            } catch (Exception e) {
                getLogger().error("reverse cmd("+cmd.intValue()+")  errorMsg:", e);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            m.release();
        }
    }


    public void showAllSessions() {
        StringBuilder sb = new StringBuilder();
        sb.append("-------------------------------------------\r\n");
        getSessionAll().forEach(s -> {
            sb.append("--------------\r\n");
            sb.append(String.format("session[%s] \r\n", s.getAgentTgtId()));
            s.getConncs().forEach(c -> {
                sb.append(String.format("connc[%s] \r\n", c.getUuid()));
            });
            sb.append("--------------\r\n");
        });
        sb.append("-------------------------------------------\r\n");
        logger.info("{}", sb.toString());
    }


    public void sendReconncMsg(String gwId, String tgtGwId, String tgtServerId, Integer conncId, Integer tgtConncId, String secret) {
        sendMsg(gwId, tgtServerId, conncId, createBaseMsgWithMiniHeadParams(Type.CTRL_CUSTOM, CTRL_EXT_TYPE_RECONNC, tgtServerId, tgtGwId, tgtConncId, secret));
    }

    public void sendRegularMsg(String gwId, String serverId, Integer conncId, String regularParam) {
        sendMsg(gwId, serverId, conncId, createBaseMsgWithMiniHeadParams(Type.CTRL_CUSTOM, CTRL_EXT_TYPE_REGULARIZE, regularParam));
    }

    public void sendHeartbeat(Channel channel) {
        Attribute<Connc<ChannelHandlerContext>> attr = channel.attr(AttributeKey.valueOf(ATTR_NAME_CONNC));
        Connc<ChannelHandlerContext> channelHandlerContextConnc = attr.get();
        BaseMsg<ByteBuf> baseMsgWithMiniHeadParams = createBaseMsgWithMiniHeadParams(Type.CTRL_HEART_BEAT, ExtType.CTRL_HEART_BEAT_EXT_TYPE_BASIC);
        channelHandlerContextConnc.sendMsg(baseMsgWithMiniHeadParams);
    }

    public void sendMsg(String gwId, String serverId, Integer conncId, BaseMsg<ByteBuf> m) {
        String sessionId = gwId + serverId;
        MultiConncSession session = getSession(sessionId);
        if (session == null) {
            logger.info("cannt find session with id[{}]", sessionId);
            return;
        }
        Connc<Object> connc = session.getConncById(conncId);
        if (connc == null) {
            logger.info("cannt find connc with id[{}]", sessionId);
            return;
        }
        connc.sendMsg(m);

    }

    public CompletableFuture<Void> req(Channel channel, int intCmd) {
        return CompletableFuture.runAsync(() -> {
            Attribute<Connc<ChannelHandlerContext>> attr = channel.attr(AttributeKey.valueOf(ATTR_NAME_CONNC));
            Connc<ChannelHandlerContext> channelHandlerContextConnc = attr.get();
            BaseMsg<ByteBuf> m = createBaseMsgWithMiniHeadParams(BaseMsgTypes.Type.BIZ_MSG, 0, ExtType.BIZ_MSG_EXT_TYPE_UNICAST, new byte[0], 3, intCmd);
            channelHandlerContextConnc.sendMsg(m);
        });

    }

    public CompletableFuture<Void> req(Channel channel, Integer cmd, Object sendBytes) {
        return CompletableFuture.runAsync(() -> {
            Attribute<Connc<ChannelHandlerContext>> attr = channel.attr(AttributeKey.valueOf(ATTR_NAME_CONNC));
            Connc<ChannelHandlerContext> channelHandlerContextConnc = attr.get();
            byte[] encode = new byte[0];
            try {
                encode = projectSGSJProtobufCodec.encode(sendBytes);
                BaseMsg<ByteBuf> m = createBaseMsgWithMiniHeadParams(BaseMsgTypes.Type.BIZ_MSG, 0, ExtType.BIZ_MSG_EXT_TYPE_UNICAST, encode, 3, cmd);
                channelHandlerContextConnc.sendMsg(m);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, channel.eventLoop());
    }

    @Override
    public Logger getLogger() {
        if (this.logger == null) {
            this.logger = LoggerFactory.getLogger(this.getClass());
        }
        return this.logger;
    }

}
