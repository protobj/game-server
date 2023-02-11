package io.protobj.mock;

import io.netty.channel.Channel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.protobj.mock.config.BaseConfig;
import io.protobj.mock.net.MockConnect;
import io.protobj.mock.net.RqstFuture;
import io.protobj.mock.net.StatisticsInfo;
import io.protobj.mock.plan.LoadOrCrePlan;
import io.protobj.mock.plan.Plan;
import io.protobj.mock.report.ReportCmdVO;
import io.protobj.mock.report.ReportVO;
import io.protobj.thread.ExecutorGroup;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created on 2021/5/20.
 *
 * @author chen qiang
 */
public class MockContext implements IMockContext {

    public static AttributeKey<MockConnect> mockAttributeKey = AttributeKey.newInstance("mockConnect");

    public BaseConfig config;

    public static final Logger logger = getLogger();

    public Map<String, MockConnect> connectMap = new ConcurrentHashMap<>();
    public static StatisticsInfo statisticsInfo = new StatisticsInfo();

    public ExecutorGroup executorGroup;

    public MockContext() {

        try {
            executorGroup = new ExecutorGroup(new ThreadGroup("MOCK"), "MOCK", Runtime.getRuntime().availableProcessors() - 1);
            initHandler();
            initNet();
            statisticsInfo.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private static Map<Integer, String> codeMap = new HashMap<>();

    public void initHandler() throws Exception {

        Runtime.getRuntime().addShutdownHook(new Thread(this::recordAndStop));
    }

    public String getCodeDesc(int code) {
        return codeMap.getOrDefault(code, "unknown");
    }

    public void initNet() throws InstantiationException, IllegalAccessException {

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
            if (/*TODO */code >= 10000) {
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

        });
    }


    public void executePlan(Plan plan, MockConnect connect) {
        if (plan != null) {
            ScheduledExecutorService executor = connect.executor();
            executor.schedule(() -> {
                plan.execute(connect).subscribeOn(connect.getScheduler()).subscribe(new Observer<Integer>() {
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
            Files.write(Paths.get(property + "\\mock_" + MockApplication.curType + "_" + new Date() + ".csv"), report.toCsv().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {

        //TODO net shutDown
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
