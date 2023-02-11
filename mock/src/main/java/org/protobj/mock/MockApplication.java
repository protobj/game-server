package org.protobj.mock;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.guangyu.cd003.projects.common.msg.RespRawData;
import com.guangyu.cd003.projects.mock.config.BaseConfig;
import com.guangyu.cd003.projects.mock.net.MockConnect;
import com.guangyu.cd003.projects.mock.net.StatisticsInfo;
import com.guangyu.cd003.projects.mock.plan.*;
import com.guangyu.cd003.projects.mock.report.ReportVO;
import com.pv.common.utilities.common.GsonUtil;
import com.pv.common.utilities.serialization.protostuff.ProtostuffUtil;
import jodd.http.HttpRequest;
import jodd.http.HttpResponse;

import java.io.IOException;
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
        normal(() -> {
            LoadOrCrePlan loadOrCrePlan = new LoadOrCrePlan();
            loadOrCrePlan.nextPlan(new RoleInfoPlan()).nextPlan(new ActionPlan());
            return loadOrCrePlan;
        }), //gm创建对应等级账号
        gm_cre(() -> {
            LoadOrCrePlan loadOrCrePlan = new LoadOrCrePlan();
            loadOrCrePlan.nextPlan(new RoleInfoPlan()).nextPlan(new GMPlan()).nextPlan(new ActionPlan());
            return loadOrCrePlan;
        }), //登录登出
        login_out(LoginLogoutPlan::new), //注册
        cre(() -> {
            LoadOrCrePlan loadOrCrePlan = new LoadOrCrePlan();
            loadOrCrePlan.nextPlan(new RoleInfoPlan()).nextPlan(new JoinLeaguePlan());
            return loadOrCrePlan;
        }), //随机请求
        random(() -> {
            LoadOrCrePlan loadOrCrePlan = new LoadOrCrePlan();
            loadOrCrePlan.nextPlan(new RoleInfoPlan()).nextPlan(new GMPlan()).nextPlan(new RandomRqstPlan());
            return loadOrCrePlan;
        }),
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

        new RespRawData();


        startLocal();

//        startRemote();

//        startSingle();
    }

    public static MockConnect startSingle() {
        curType = MockType.normal;
        Supplier<Plan> planSupplier = curType.planSupplier;
        BaseConfig config = BaseConfig.valueOf(curType, null);
        System.err.println(GsonUtil.toJSONString(config));
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

    public static MockManagerTcpServer.MockInstanceData rqst(String url, String path, String data) throws IOException {
        String[] split = url.split(":");
        HttpRequest request = new HttpRequest();
        request.connectionKeepAlive(false).host(split[0]).port(Integer.parseInt(split[1])).path(path);
        request.connectionTimeout(50000).timeout(100000);
        if (data == null) {
            request.method("GET");
        }
        request.protocol("http");
        if (data != null) {
            request.method("POST");
            byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
            request.contentLength(bytes.length);
            request.body(bytes, "Content-Type: application/json; charset=utf-8");
        }
        HttpResponse send = request.send();
        byte[] bytes = send.bodyBytes();
        return ProtostuffUtil.deser(bytes, MockManagerTcpServer.MockInstanceData.class);
    }
//312963950834618368
    private static void startRemote0(final Map<String, String> baseConfig) {
        Runnable runnable = () -> {
            clearCache();
            curType = MockType.valueOf(baseConfig.get("mockType"));
            Supplier<Plan> planSupplier = curType.planSupplier;
            MockContext.statisticsInfo = new StatisticsInfo();
            BaseConfig config = BaseConfig.valueOf(curType, baseConfig);
            System.err.println(GsonUtil.toJSONString(config));
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
        GMPlan.completeCount.set(0);

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
        System.err.println(GsonUtil.toJSONString(config));
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
