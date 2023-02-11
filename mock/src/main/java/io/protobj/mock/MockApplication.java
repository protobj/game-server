package io.protobj.mock;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import io.protobj.mock.config.BaseConfig;
import io.protobj.mock.net.MockConnect;
import io.protobj.mock.net.StatisticsInfo;
import io.protobj.mock.plan.ActionPlan;
import io.protobj.mock.plan.LoadOrCrePlan;
import io.protobj.mock.plan.Plan;
import io.protobj.mock.report.ReportVO;
import io.protobj.util.Jackson;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class MockApplication {

    public enum MockType {
        //正常玩
        normal(() -> null), //gm创建对应等级账号
        gm_cre(() -> null), //登录登出
        login_out(() -> null), //注册
        cre(() -> null), //随机请求
        random(() -> null),
        ;

        MockType(Supplier<Plan> planSupplier) {
            this.planSupplier = planSupplier;
        }

        final Supplier<Plan> planSupplier;
    }

    public static MockType curType;
    public static MockContext mockContext;

    public static void main(String[] args) throws Exception {
        System.setProperty("Log4jContextSelector", "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector");
        System.setProperty("cq.mock", "true");
//        BlockHound.install(new BlockHoundIntegration() {
//            @Override
//            public void applyTo(BlockHound.Builder builder) {
//                builder.allowBlockingCallsInside("com.baidu.bjf.remoting.protobuf.ProtobufProxy", "create");
//                builder.allowBlockingCallsInside("com.guangyu.cd003.projects.common.msg.RespRawData", "<clinit>");
//            }
//        });


        startLocal();

//        startRemote();

//        startSingle();
    }

    public static MockConnect startSingle() {
        curType = MockType.normal;
        Supplier<Plan> planSupplier = curType.planSupplier;
        BaseConfig config = BaseConfig.valueOf(curType, null);
        System.err.println(Jackson.INSTANCE.encode(config));
        MockContext mockContext = new MockContext();
        mockContext.config = config;
        MockConnect mockConnect = new MockConnect(mockContext, "454353", config.getServerId(), planSupplier);
        mockContext.connect(mockConnect);
        return mockConnect;
    }

    private static void startRemote() throws IOException {
        Properties properties = new Properties();
        properties.load(MockApplication.class.getClassLoader().getResourceAsStream("server.properties"));
        String hostName = properties.getProperty("host");
        String port = properties.getProperty("port");
        Client client = new Client();
        client.getKryo().setRegistrationRequired(false);
        client.start();
        client.addListener(new Listener() {
            @Override
            public void connected(Connection connection) {
                super.connected(connection);
                System.err.println("已连接");
            }

            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof MockManagerTcpServer.Stop) {
                    StatisticsInfo statisticsInfo = MockContext.statisticsInfo;
                    stopRemote0();
                    ReportVO reportVO = mockContext.createReportVO(statisticsInfo);
                    connection.sendTCP(reportVO);
                } else if (object instanceof MockManagerTcpServer.Start) {
                    startRemote0(((MockManagerTcpServer.Start) object).baseConfig);
                }
            }

            @Override
            public void disconnected(Connection connection) {
                System.err.println("断开连接");
                new Thread(() -> {
                    while (!client.isConnected()) {
                        try {
                            client.reconnect(10000);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });
        while (!client.isConnected()) {
            try {
                client.connect(10000, hostName, Integer.parseInt(port));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void stopRemote0() {
        if (mockContext != null) {
            mockContext.stop();
        }
    }

    //312963950834618368
    private static void startRemote0(final Map<String, String> baseConfig) {
        Runnable runnable = () -> {
            clearCache();
            curType = MockType.valueOf(baseConfig.get("mockType"));
            Supplier<Plan> planSupplier = curType.planSupplier;
            MockContext.statisticsInfo = new StatisticsInfo();
            BaseConfig config = BaseConfig.valueOf(curType, baseConfig);
            System.err.println(Jackson.INSTANCE.encode(config));
            mockContext = new MockContext();
            mockContext.config = config;
            start(mockContext, config, planSupplier);
        };
        new Thread(runnable).start();
    }

    private static void clearCache() {
        LoadOrCrePlan.startTime.set(0);
        LoadOrCrePlan.onlineCount.set(0);
        LoadOrCrePlan.endTime.set(0);
        curType = null;
        mockContext = null;
        if (MockContext.statisticsInfo != null) {
            MockContext.statisticsInfo = null;
        }
        System.gc();

    }

    private static void startLocal() {
        curType = MockType.cre;
        Supplier<Plan> planSupplier = curType.planSupplier;
        BaseConfig config = BaseConfig.valueOf(curType, null);
        System.err.println(Jackson.INSTANCE.encode(config));
        MockContext mockContext = new MockContext();
        mockContext.config = config;
        start(mockContext, config, planSupplier);
    }

    protected static void start(MockContext mockContext, BaseConfig config, Supplier<? extends Plan> supplier) {
        String prefix = config.getPrefix();
        int turn = config.getTurn();
        int count = config.getCount();
        int turnInterval = config.getTurnInterval();
        List<List<String>> accountList = new ArrayList<>();
        for (int i = 1; i <= turn; i++) {
            List<String> accounts = new ArrayList<>();
            for (int j = 1; j <= count; j++) {
                String account = prefix + ((i - 1) * count + j);
                accounts.add(account);
            }
            accountList.add(accounts);
        }
        while (!accountList.isEmpty()) {
            List<String> accounts = accountList.remove(0);
            for (String account : accounts) {
                MockConnect mockConnect = new MockConnect(mockContext, account, config.getServerId(), supplier);
                mockContext.connect(mockConnect);
            }
            try {
                TimeUnit.MILLISECONDS.sleep(turnInterval);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}
