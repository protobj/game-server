package org.protobj.mock.net;

import static com.guangyu.cd003.projects.mock.MockContext.statisticsInfo;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Supplier;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.guangyu.cd003.projects.common.cons.Commands;
import com.guangyu.cd003.projects.common.msg.RespFully;
import com.guangyu.cd003.projects.common.msg.RespFullyLst;
import com.guangyu.cd003.projects.common.msg.RespRawDatalizable;
import com.guangyu.cd003.projects.gs.module.chat.cons.ChatType;
import com.guangyu.cd003.projects.gs.module.chat.msg.RqstChat;
import com.guangyu.cd003.projects.gs.module.expdn.cons.RespTypeExpdn;
import com.guangyu.cd003.projects.gs.module.expdn.msg.RespExpdnOp;
import com.guangyu.cd003.projects.mock.MockContext;
import com.guangyu.cd003.projects.mock.RespHandler;
import com.guangyu.cd003.projects.mock.module.barracks.BarracksData;
import com.guangyu.cd003.projects.mock.module.city.CityData;
import com.guangyu.cd003.projects.mock.module.depot.DepotData;
import com.guangyu.cd003.projects.mock.module.hero.HeroData;
import com.guangyu.cd003.projects.mock.module.hospital.HospitalData;
import com.guangyu.cd003.projects.mock.module.league.LeagueData;
import com.guangyu.cd003.projects.mock.module.legion.LegionData;
import com.guangyu.cd003.projects.mock.module.mail.MailData;
import com.guangyu.cd003.projects.mock.module.product.ProductData;
import com.guangyu.cd003.projects.mock.module.role.RoleData;
import com.guangyu.cd003.projects.mock.module.scene.SceneData;
import com.guangyu.cd003.projects.mock.plan.Plan;
import com.pv.common.utilities.common.CommonUtil;
import com.pv.common.utilities.common.GsonUtil;
import com.pv.framework.gs.core.msg.CodeGameServerSys;
import com.pv.terminal.netty.NettyGwTmnl;

import io.netty.channel.Channel;
import io.netty.channel.DefaultChannelPromise;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Created on 2021/5/20.
 *
 * @author chen qiang
 */
public class MockConnect extends NettyGwTmnl {

    //    private final static Logger logger = LoggerFactory.getLogger(MockConnect.class);
    Logger logger = getLogger();

    private String uid;
    private String serverId;
    private Channel channel;

    public MockContext mockContext;

    /***************************************游戏内数据START***************************************************/
    public long sysTime;

    public LegionData LEGION_DATA = new LegionData();
    public SceneData SCENE_DATA = new SceneData();
    public CityData CITY_DATA = new CityData();
    public HeroData HERO_DATA = new HeroData();
    public RoleData ROLE_DATA = new RoleData();
    public BarracksData BARRACKS_DATA = new BarracksData();
    public HospitalData HOSPITAL_DATA = new HospitalData();

    public ProductData PRODUCT_DATA = new ProductData();

    public MailData MAIL_DATA = new MailData();

    public DepotData DEPOT_DATA = new DepotData();

    public LeagueData LEAGUE_DATA = new LeagueData();
    //上一次收到的消息
    public Map<Integer, RespRawDatalizable> LAST_RECV_MSGS = CommonUtil.createMap();

    /***************************************游戏内数据END***************************************************/


    public Map<Integer, List<RqstFuture>> rqstFutureMap = CommonUtil.createMap();

    public Supplier<? extends Plan> planSupplier;

    private static final Map<ScheduledExecutorService, Scheduler> schedulerMap = CommonUtil.createMap();

    private ScheduledExecutorService executor;
    private Scheduler scheduler;

    private ScheduledFuture<?> heartbeatFuture;

    private boolean planEnd;

    public MockConnect(MockContext mockContext, String uid, String serverId, Supplier<? extends Plan> planSupplier) {
        this.mockContext = mockContext;
        this.uid = uid;
        this.serverId = serverId;
        this.planSupplier = planSupplier;
    }

    public String getUid() {
        return uid;
    }

    public void setChannel(Channel channel, ScheduledExecutorService executor) {
        this.channel = channel;
        this.executor = executor;
        scheduler = schedulerMap.computeIfAbsent(executor, t ->RxJavaPlugins.createExecutorScheduler(t, false, false));
        heartbeatFuture = executor.scheduleAtFixedRate(() -> {
            if (channel != null && channel.isActive()) {
                MockContext.mockNettyGwFrtndTmnlCommHandler.sendHeartbeat(channel);
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public Channel getChannel() {
        return channel;
    }

    public RqstFuture send(int cmd, Object sendBytes) {
        String name = Thread.currentThread().getName();
        if (!name.contains("MockExecutorGroup")) {
            Thread.dumpStack();
            throw new UnsupportedOperationException("没有在线程中执行发送逻辑");
        }
        RqstFuture rqstFuture = new RqstFuture(cmd);
        try {
            List<RqstFuture> before = rqstFutureMap.computeIfAbsent(cmd, t -> new CopyOnWriteArrayList<>());
            if (before.size() > 0) {
                Iterator<RqstFuture> iterator = before.iterator();
                while (iterator.hasNext()) {
                    RqstFuture beforeRqst = iterator.next();
                    if (beforeRqst.rqstTime != 0 && beforeRqst.rqstTime + TimeUnit.MINUTES.toMillis(5) < System.currentTimeMillis()) {
                        if (cmd != 511 && cmd != 512 && cmd != 304) {
                            logger.error("请求超时：{} {} {}", beforeRqst.rqstTime, cmd, ExceptionUtils.getStackTrace(new Exception()));
                            beforeRqst.completeExceptionally(new CodeException(cmd, CodeGameServerSys.UNKNOWN_SYS_EXCEPTION.getCode()));
                        } else {
                            beforeRqst.completeExceptionally(new CodeException(cmd, CodeGameServerSys.UNKNOWN_SYS_EXCEPTION.getCode()));
                        }
                        iterator.remove();
                    } else {
//                    throw new UnsupportedOperationException("不支持同时发送两个相同cmd请求 " + cmd);
                    }
                }
            }
            CompletableFuture<Void> encodeFuture;
            if (sendBytes == null) {
                encodeFuture = MockContext.mockNettyGwFrtndTmnlCommHandler.req(channel, cmd);
            } else {
                encodeFuture = MockContext.mockNettyGwFrtndTmnlCommHandler.req(channel, cmd, sendBytes);
            }
            before.add(rqstFuture);
            encodeFuture.thenRun(() -> {
                rqstFuture.setRqstTime(System.currentTimeMillis());
                statisticsInfo.rqst(cmd);
            }).exceptionally(e -> {
                rqstFuture.completeExceptionally(e);
                return null;
            });
            if (mockContext.config.isPrintLog(cmd)) {
                if (sendBytes == null) {
                    logger.info("mock send:{}", cmd);
                } else {
                    logger.info("mock send:{} {}:{}", cmd, sendBytes.getClass().getSimpleName(), GsonUtil.toJSONString(sendBytes));
                }
            }
        } catch (UnsupportedOperationException e) {
            rqstFuture.completeExceptionally(e);
        }

        return rqstFuture;
    }

    public CompletableFuture<Integer> sendChatCMD(String cmdSign, Object... params) {
        RqstChat rqstChat = new RqstChat();
        rqstChat.channel = ChatType.WORLD.getValue();
        StringBuilder paramStr = new StringBuilder();
        paramStr.append("/cmd").append(" ").append(cmdSign);
        for (int i = 0; i < params.length; i++) {
            paramStr.append(" ").append(params[i]);
        }
        rqstChat.msg = paramStr.toString();
        if (!this.isSending(Commands.CHAT_SEND_MSG_CONST)) {
            System.out.println("send:" + rqstChat.msg);
            return send(Commands.CHAT_SEND_MSG_CONST, rqstChat);
        }
        return CompletableFuture.completedFuture(0);
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


    public CompletableFuture<Integer> tryClose() {
        DefaultChannelPromise promise = new DefaultChannelPromise(channel);
        CompletableFuture<Integer> closeFuture = new CompletableFuture<>();
        channel.close(promise).addListener(future -> {
            logger.info("login out!! {}", uid);
            mockContext.removeConnect(channel);
            close();
            closeFuture.complete(0);
        });
        return closeFuture;
    }

    public void close() {
        if (heartbeatFuture != null) {
            heartbeatFuture.cancel(true);
        }
    }

    public Logger getLogger() {
        if (this.logger == null) {
            this.logger = LoggerFactory.getLogger(MockConnect.class);
        }
        return this.logger;
    }


    public void handleData(int cmd, RespFullyLst result) {
        for (RespFully resp : result.resps) {
            for (Map.Entry<Integer, RespRawDatalizable> entry : resp.opAndInfos.entrySet()) {
                Integer subCmd = entry.getKey();
                RespRawDatalizable msg = entry.getValue();
                RespHandler respHandler = RespHandler.RESP_HANDLER_MAP.get(subCmd);
                if (respHandler == null) {
                    logger.warn("cmd:{} subCmd:{} msg:{} 没有处理", cmd, subCmd, msg.getClass().getSimpleName());
                    System.exit(-1);
                } else {
                    if (mockContext.config.isPrintLog(cmd)) {
                        logger.info("mock recv cmd:{} subCmd:{}  {}:{}", cmd, subCmd, msg.getClass().getSimpleName(), GsonUtil.toJSONString(msg));
                    }
                    try {
                        if (msg instanceof RespExpdnOp) {
                            RespHandler.RESP_HANDLER_MAP.get(RespTypeExpdn.RESP_TYPE_EXPDN_OP).handle(this, msg, cmd);
                        } else {
                            respHandler.handle(this, msg, cmd);
                        }
                    } catch (Exception e) {
                        logger.error("handleData", e);
                    }
                }
            }
            if (resp.sysTime != null && resp.sysTime.value > 0) {
                sysTime = resp.sysTime.value;
                logger.info("mock recv sysTime:{}", new Date(resp.sysTime.value).toLocaleString());
            }
        }
    }

    public ScheduledExecutorService executor() {
        return executor;
    }


    public boolean isSending(int rqstCmd) {
//        List<RqstFuture> rqstFutures = rqstFutureMap.get(rqstCmd);
//        return CollectionUtils.isNotEmpty(rqstFutures);
        return false;
    }

    public boolean isOffLine() {
        return channel == null || !channel.isActive();
    }

    public void setPlanEnd() {
        this.planEnd = true;
    }

    public boolean isPlanEnd() {
        return planEnd;
    }
}
