package org.protobj.mock;

import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.guangyu.cd003.projects.common.msg.RespFullyLst;
import com.guangyu.cd003.projects.mock.config.BaseConfig;
import com.guangyu.cd003.projects.mock.executor.ExecutorGroup;
import com.guangyu.cd003.projects.mock.net.CodeException;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.net.RqstFuture;
import com.guangyu.cd003.projects.mock.net.StatisticsInfo;
import com.guangyu.cd003.projects.mock.plan.LoadOrCrePlan;
import com.guangyu.cd003.projects.mock.plan.Plan;
import com.guangyu.cd003.projects.mock.report.ReportCmdVO;
import com.guangyu.cd003.projects.mock.report.ReportVO;
import com.pv.common.net.netty.NettyConfig;
import com.pv.common.utilities.datetime.NDateUtil;
import com.pv.common.utilities.exception.ICode;
import com.pv.common.utilities.reflc.ReflcUtil;
import com.pv.framework.gs.core.cfg.core.MockConfigLoader;
import com.pv.framework.gs.core.msg.CodeGameServerSys;
import com.pv.framework.gs.core.util.RandomUtils;
import com.pv.terminal.netty.NettyGwFrtndTmnlTcpClient;
import com.pv.terminal.netty.NettyGwTmnl;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created on 2021/5/20.
 *
 * @author chen qiang
 */
public class MockContext extends NettyGwTmnl implements IMockContext {

    public static AttributeKey<MockConnect> mockAttributeKey = AttributeKey.newInstance("mockConnect");

    public BaseConfig config;

    public static final Logger logger = getLogger();

    public NettyGwFrtndTmnlTcpClient nettyGwFrtndTmnlTcpClient;
    public static MockNettyGwFrtndTmnlCommHandler mockNettyGwFrtndTmnlCommHandler;
    public Map<String, MockConnect> connectMap = Maps.newConcurrentMap();
    public static StatisticsInfo statisticsInfo = new StatisticsInfo();

    public ExecutorGroup executorGroup;

    public MockContext() {

        try {
            executorGroup = new ExecutorGroup(Runtime.getRuntime().availableProcessors() * 2);
            initHandler();
            initNet();
            statisticsInfo.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<Integer, String> codeMap = new HashMap<>();

    public void initHandler() throws Exception {
        if (codeMap.isEmpty()) {
            new MockConfigLoader().loadConfig();
            Set<Class<? extends ICode>> classes = ReflcUtil.forClassSubTypeOf("com.guangyu.cd003.projects", ICode.class);
            for (Class<? extends ICode> aClass : classes) {
                Class enumClass = aClass;
                Object[] enumConstants = enumClass.getEnumConstants();
                for (int i = 0; i < enumConstants.length; i++) {
                    ICode code = (ICode) enumConstants[i];
                    codeMap.put(code.getCode(), code.toString());
                }
            }
            Set<Class<? extends RespHandler>> classes1 = ReflcUtil.forClassSubTypeOf("com.guangyu.cd003.projects.mock.module", RespHandler.class);
            for (Class<? extends RespHandler> aClass : classes1) {
                aClass.newInstance().init();
            }
        }
        Runtime.getRuntime().addShutdownHook(new Thread(this::recordAndStop));
    }

    public String getCodeDesc(int code) {
        return codeMap.getOrDefault(code, "unknown");
    }

    public void initNet() throws InstantiationException, IllegalAccessException {
        String id = "client_1001";
        String name = "client_1001";
        String group = "client";
        NettyConfig nettyConfig = new NettyConfig();
        nettyConfig.setConncTimeout((int) TimeUnit.SECONDS.toMillis(100));
        mockNettyGwFrtndTmnlCommHandler = new MockNettyGwFrtndTmnlCommHandler(this);
        this.nettyGwFrtndTmnlTcpClient = new NettyGwFrtndTmnlTcpClient(id, name, group, nettyConfig, decoderSupplier, encoderSupplier, mockNettyGwFrtndTmnlCommHandler);
        new Thread(() -> {
            while (!record) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (connectMap.size() > 0) {
                    logger.warn("onlineCount: {}", connectMap.size());
                }
            }
        }).start();
    }

    @Override
    public void onEventBeforeDecode(Channel channel, Integer cmd, int code) {
        MockConnect connect = channel.attr(mockAttributeKey).get();
        List<RqstFuture> rqstFutures = connect.rqstFutureMap.computeIfAbsent(cmd, t -> new CopyOnWriteArrayList<>());
        if (rqstFutures.isEmpty()) {
            return;
        }
        RqstFuture rqstFuture = rqstFutures.get(0);
        boolean success = code == 0;
        if (success) {
            statisticsInfo.resp(cmd, rqstFuture.getRqstTime());
        } else {
            if (code == CodeGameServerSys.UNKNOWN_SYS_EXCEPTION.getCode() || code >= 10000) {
                statisticsInfo.respErr(cmd, rqstFuture.getRqstTime(), code);
            } else {
                statisticsInfo.resp(cmd, rqstFuture.getRqstTime());
            }
        }
    }

    public void onEvent(Channel channel, Integer cmd, Object result) {
        MockConnect connect = channel.attr(mockAttributeKey).get();
        connect.executor().execute(() -> {
            if (config.isPrintLog(cmd) && result instanceof Integer && !result.equals(0)) {
                logger.info("recv error msg cmd:{} code:{}", cmd, getCodeDesc((Integer) result));
            }
            List<RqstFuture> rqstFutures = connect.rqstFutureMap.computeIfAbsent(cmd, t -> new CopyOnWriteArrayList<>());
            if (rqstFutures.isEmpty()) {
                return;
            }
            RqstFuture rqstFuture = rqstFutures.remove(0);

            if (result instanceof RespFullyLst) {
                connect.handleData(cmd, (RespFullyLst) result);
            }
            if (rqstFuture != null) {
                if (result.equals(0) || result instanceof RespFullyLst) {
                    rqstFuture.complete(0);
                } else {
                    rqstFuture.completeExceptionally(new CodeException(cmd, (Integer) result));
                }
            }
        });
    }


    public void executePlan(Plan plan, MockConnect connect) {
        if (plan != null) {
            ScheduledExecutorService executor = connect.executor();
            executor.schedule(() -> {
                plan.execute(connect)
                        .subscribeOn(connect.getScheduler())
                        .subscribe(new Observer<Integer>() {
                            @Override
                            public void onSubscribe(@NonNull Disposable d) {

                            }

                            @Override
                            public void onNext(@NonNull Integer integer) {
                            }

                            @Override
                            public void onError(@NonNull Throwable e) {
                                logger.error("onError ", e);
                            }

                            @Override
                            public void onComplete() {
                                executePlan(plan.getNextPlan(), connect);
                            }
                        });
            }, 0, TimeUnit.MILLISECONDS);
        } else {
            logger.info("任务执行完成 {}", connect.getUid());
            connect.setPlanEnd();
        }
    }

    public void connect(MockConnect mockConnect) {
        String url = config.getGateway()[RandomUtils.nextInt(config.getGateway().length)];
        String[] split = url.split(":");
        logger.info("开始连接：{} {}", mockConnect.getUid(), url);
        nettyGwFrtndTmnlTcpClient.startupWithBindedAndShutdownDeal(split[0], Integer.parseInt(split[1]), (cli, f) -> {
            io.netty.util.concurrent.Future nettyFuture = (Future) f;
            if (nettyFuture instanceof ChannelFuture && nettyFuture.isSuccess()) {
                ChannelFuture channelFuture = (ChannelFuture) f;
                Channel channel = channelFuture.channel();
                mockConnect.setChannel(channel, executorGroup.next());
                channel.attr(mockAttributeKey).set(mockConnect);
                logger.info("连接成功->{}:", mockConnect.getUid());
                String serverId = mockConnect.getServerId();
                channel.attr(AttributeKey.valueOf(MockNettyGwFrtndTmnlCommHandler.ATTR_NAME_SERVER_ID)).set(serverId);
                channel.attr(AttributeKey.valueOf(MockNettyGwFrtndTmnlCommHandler.ATTR_NAME_REGULAR_PARAM)).set(mockConnect.getUid());
            } else {
                logger.error("连接失败-> {} {}", mockConnect.getUid(), url);
            }
        }, (cli, f) -> {
            logger.error("shutdown : 连接失败：{} {}", mockConnect.getUid(), url);
            recordAndStop();
            System.exit(-1);
        });
    }

    public Map<String, MockConnect> getConnectMap() {
        return connectMap;
    }

    public void removeConnect(Channel channel) {
        Attribute<MockConnect> attr = channel.attr(mockAttributeKey);
        MockConnect mockConnect = attr.getAndSet(null);
        if (mockConnect != null) {
            logger.error("连接断开 :{}", mockConnect.getUid());
            connectMap.remove(mockConnect.getUid());
            mockConnect.close();
        }
    }

    @Override
    public void onRegularSucc(Channel channel) {
        MockConnect mockConnect = channel.attr(MockContext.mockAttributeKey).get();
        executePlan(mockConnect.planSupplier.get(), mockConnect);
    }

    public static Logger getLogger() {
        return LoggerFactory.getLogger(MockContext.class);
    }

    private boolean record = false;

    public void recordAndStop() {
        if (record) {
            return;
        }
        stop();

        record = true;
        StatisticsInfo statisticsInfo1 = statisticsInfo;
        ReportVO report = createReportVO(statisticsInfo1);
        String property = System.getProperty("user.dir");
        try {
            Files.write(report.toCsv().getBytes(StandardCharsets.UTF_8), new File(property + "\\mock_" + MockApplication.curType + "_" + NDateUtil.sdf.get().format(new Date()) + ".csv"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        if (nettyGwFrtndTmnlTcpClient != null) {
            nettyGwFrtndTmnlTcpClient.shutdown((k, v) -> {
            });
        }
        if (executorGroup != null) {
            executorGroup.shutdown();
        }

    }

    public ReportVO createReportVO(StatisticsInfo statisticsInfo1) {
        ReportVO report = new ReportVO();

        report.setOnlineCount(LoadOrCrePlan.onlineCount.get());


        double sec = TimeUnit.SECONDS.toMillis(1);
        if (LoadOrCrePlan.startTime.get() != 0 && LoadOrCrePlan.endTime.get() != 0) {
            double totalTime = (LoadOrCrePlan.endTime.get() - LoadOrCrePlan.startTime.get() - (config.getTurn() - 1) * config.getTurnInterval()) / sec;
            double countPerSec = LoadOrCrePlan.onlineCount.get() / totalTime;
            report.setRegisterCountPerSec((int) countPerSec);
        }
        double time = (statisticsInfo1.endTime - statisticsInfo1.startTime) / sec;
        report.setRqstPerSec((int) (statisticsInfo1.msgCount.get() / time));

        Map<Integer, List<StatisticsInfo.CmdStatisticsInfo>> collect = statisticsInfo1.cmdStatisticsInfoMap.values().stream().collect(Collectors.groupingBy(t -> t.cmd / 100 * 100));
        for (List<StatisticsInfo.CmdStatisticsInfo> values : collect.values()) {
            for (StatisticsInfo.CmdStatisticsInfo value : values) {
                int key = value.cmd;
                ReportCmdVO reportCmdVO = new ReportCmdVO();
                reportCmdVO.setCmd(key);
                reportCmdVO.setRqstCount(value.rqstCount.get());
                reportCmdVO.setSuccessCount(value.successCount.get());
                reportCmdVO.setAverageRespTime1(value.getAverage1());
                reportCmdVO.setAverageRespTime2(value.getAverage2());
                reportCmdVO.setMinRespTime(value.getMin());
                reportCmdVO.setMaxRespTime(value.getMax());
                reportCmdVO.setErrRate(value.getErrorRate());
                reportCmdVO.setRqstPerSec1(value.getThroughput1());
                reportCmdVO.setRqstPerSec2(value.getThroughput2());
                reportCmdVO.setErrCode(value.errorCodeMap);
                report.getReportCmdList().add(reportCmdVO);
            }
        }
        return report;
    }

}
