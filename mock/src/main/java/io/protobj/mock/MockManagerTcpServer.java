package io.protobj.mock;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import io.protobj.mock.config.BaseConfig;
import io.protobj.mock.report.ReportVO;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class MockManagerTcpServer {

    public static final int IDLE = 0;
    public static final int START = 1;
    public static final int STARTING = 2;
    public static final int STOP = 3;
    public static final int COLLECTED = 4;

    public static class MockManagerData {
        private Map<Integer, MockInstanceData> mockInstanceData = new HashMap<>();

        public Map<Integer, MockInstanceData> getMockInstanceData() {
            return mockInstanceData;
        }

        public void setMockInstanceData(Map<Integer, MockInstanceData> mockInstanceData) {
            this.mockInstanceData = mockInstanceData;
        }
    }

    public static class Start {
        Map<String, String> baseConfig;
    }

    public static class Stop {
    }

    public static class MockInstanceData {
        public String hostName;
        public ReportVO report;

        private Map<String, String> baseConfig;

        private long activeTime;

        private transient int connectionId;

        public MockInstanceData(String hostName, int connectionId) {
            this.hostName = hostName;
            this.connectionId = connectionId;
        }

        public String getHostName() {
            return hostName;
        }

        public void setHostName(String hostName) {
            this.hostName = hostName;
        }

        public ReportVO getReport() {
            return report;
        }

        public void setReport(ReportVO report) {
            this.report = report;
        }

        public Map<String, String> getBaseConfig() {
            return baseConfig;
        }

        public void setBaseConfig(Map<String, String> baseConfig) {
            this.baseConfig = baseConfig;
        }

        public long getActiveTime() {
            return activeTime;
        }

        public void setActiveTime(long activeTime) {
            this.activeTime = activeTime;
        }
    }

    public static void main(String[] args) throws IOException {
        Properties properties = new Properties();
        properties.load(MockManagerTcpServer.class.getClassLoader().getResourceAsStream("server.properties"));
        int port = Integer.parseInt(properties.getProperty("port"));
        MockManagerData mockManagerData = new MockManagerData();
        Server server = new Server();
        server.getKryo().setRegistrationRequired(false);
        server.bind(port);
        server.addListener(new Listener() {
            @Override
            public void connected(Connection connection) {
                String hostName = connection.getRemoteAddressTCP().getHostString();
                mockManagerData.getMockInstanceData().computeIfAbsent(connection.getID(), t -> {
                    System.err.println(hostName + " connect... ");
                    return new MockInstanceData(hostName, connection.getID());
                });
            }

            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof ReportVO) {
                    String hostName = connection.getRemoteAddressTCP().getHostString();
                    MockInstanceData mockInstanceData = mockManagerData.getMockInstanceData().get(connection.getID());
                    if (mockInstanceData != null) {
                        mockInstanceData.report = (ReportVO) object;
                        System.err.println(hostName + " collected ");
                    }
                }
            }

            @Override
            public void disconnected(Connection connection) {
                System.err.println("disconnected");
                mockManagerData.getMockInstanceData().remove(connection.getID());
            }
        });
        server.start();

        System.err.println("started on : " + port);
        Scanner scanner = new Scanner(System.in);
        while (true) {
            try {
                String next = scanner.nextLine();
                System.err.println(next);
                if (StringUtils.isEmpty(next)) {
                    continue;
                }
                if (next.startsWith("start")) {
                    String[] split = next.split(" ");
                    MockApplication.MockType mockType = MockApplication.MockType.valueOf(split[1]);
                    int count = Integer.parseInt(split[2]) / mockManagerData.getMockInstanceData().size();
                    BaseConfig baseConfig = BaseConfig.valueOf(mockType, null);
                    int prefix = 0;
                    int rolePerSec = Math.min(100, count) / mockManagerData.getMockInstanceData().size();
                    int normalCount = Integer.parseInt(split[3]);
                    for (MockInstanceData value : mockManagerData.getMockInstanceData().values()) {
                        Map<String, String> configMap = baseConfig.toMap();
                        configMap.put("turn", String.valueOf(rolePerSec == 0 ? 1 : count / rolePerSec));
                        configMap.put("count", String.valueOf(rolePerSec == 0 ? 1 : rolePerSec));
                        if (normalCount > 0 && !value.getHostName().endsWith("13")) {
                            normalCount--;
                            configMap.put("mockType", "normal");
                        } else {
                            configMap.put("mockType", split[1]);
                        }
                        configMap.put("prefix", prefix + baseConfig.getPrefix());
                        value.setBaseConfig(configMap);
                        System.err.println(configMap);
                        int connectionId = value.connectionId;
                        Start object = new Start();
                        object.baseConfig = configMap;
                        server.sendToTCP(connectionId, object);
                        value.report = null;
                        prefix++;
                    }
                } else if (next.startsWith("stop")) {
                    server.sendToAllTCP(new Stop());
                } else if (next.startsWith("collect")) {
                    List<ReportVO> reportVos = new ArrayList<>();
                    for (MockInstanceData value : mockManagerData.getMockInstanceData().values()) {
                        if (value.report != null) {
                            ReportVO report = value.getReport();
                            reportVos.add(report);
                        }
                    }
                    if (!reportVos.isEmpty()) {
                        ReportVO reportVO = reportVos.remove(0);
                        reportVO.merge(reportVos);
                        String property = System.getProperty("user.dir");
                        try {

                            Files.write(Paths.get(property + "\\mock_" + new Date() + ".csv"), reportVO.toCsv().getBytes(StandardCharsets.UTF_8));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }
}
