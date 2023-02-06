package io.protobj;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import io.protobj.resource.table.Id;
import io.protobj.resource.table.Unique;
import io.protobj.scheduler.HashedWheelTimer;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.UnknownHostException;

public class TestMain {
    public static void main(String[] args) throws Exception, IllegalAccessException, UnknownHostException {
//        HotSwapManger hotSwapManger = new HotSwapManger();
//        hotSwapManger.start(new HotSwapConfig("/home/chen/game/swap", "/home/chen/game/add",8787));
////        new Timer().schedule(new TimerTask() {
////            @Override
////            public void run() {
////                new Test().print();e
////
////                Test test = new Test();
////                test.testInner.print();
////
////                Test.TestModule testModule = new Test.TestModule();
////                testModule.print();
////                System.err.println("------------------------");
////            }
////        }, 0, 2000);

//
//        Path path = Paths.get("C:\\Users\\79871\\GolandProjects\\excel-convert\\json\\server\\testresource_single.json");
//        ObjectMapper objectMapper = new ObjectMapper();
//        JsonNode jsonNode = objectMapper.readTree(path.toFile());
//        JsonNode path1 = jsonNode.path("0.hp");
//        System.err.println(path1.toString());
//        TableContainer<Integer, TestResource> tableContainer = new TableContainer<>(TestResource.class);
//        JackSonImpl jackSon = new JackSonImpl();
//        TableContainer<Integer, TestResource> load = tableContainer.load(Path.of("C:\\Users\\79871\\GolandProjects\\excel-convert\\json\\server\\testresource.json"), jackSon);
//        System.err.println();

//
//        NettyGateServer nettyGateServer = new NettyGateServer(1);
//
//        String localhost = "localhost";
//        int port = 9999;
//        CompletableFuture<Void> gate = nettyGateServer.startTcpBackendServer(localhost, port);
//        gate.whenCompleteAsync((r, e) -> {
//            if (e != null) {
//                e.printStackTrace();
//            } else {
//                System.err.printf("gate backend[tcp] start in %s:%d%n", localhost, port);
//            }
//        }).join();
//        MutilChannelSession mutilChannelSession = new MutilChannelSession();
//        NettyGateClient nettyGateClient = new NettyGateClient(1, new SessionCache() {
//            @Override
//            public Session getSessionById(int channelId) {
//                return null;
//            }
//        });
//        for (int i = 0; i < 3; i++) {
//            CompletableFuture<Channel> client = nettyGateClient.startTcpBackendClient(localhost, port, 1);
//            Channel join = client.join();
//            mutilChannelSession.add(join);
//        }
//
//

        HashedWheelTimer hashedWheelTimer = new HashedWheelTimer(new ThreadGroup("Test-Timer"), 1000, 60,null);

        //直接执行
//        hashedWheelTimer.execute(Runnable::run, () -> {
//            System.out.println("hahshsdhfasdf");
//        });
//        hashedWheelTimer.fixedRate(Runnable::run, 1000, () -> {
//            System.err.println("fixedRate + " + System.currentTimeMillis());
//        });
//        ExecutorService executorService = Executors.newSingleThreadExecutor();
//        hashedWheelTimer.fixedDelay(executorService, 1000, () -> {
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//            System.err.println("fixedDelay + " + System.currentTimeMillis());
//        });

        hashedWheelTimer.cron(Runnable::run, "*/5 * * * * ?", () -> {
            System.err.println("cron + " + System.currentTimeMillis());
        });


        Thread.sleep(10000000);
    }

    public static class JackSonImpl implements Json {
        private ObjectMapper objectMapper = new ObjectMapper();

        public JackSonImpl() {
            objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            objectMapper.configure(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES.mappedFeature(), true);
            objectMapper.configure(JsonWriteFeature.QUOTE_FIELD_NAMES.mappedFeature(), false);
        }

        @Override
        public String encode(Object obj) {
            try {
                return objectMapper.writeValueAsString(obj);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public <T> T decode(String json, Class<T> valueType) {
            try {
                return objectMapper.readValue(json, valueType);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Object decode(String json, Type type) {
            final JavaType javaType = objectMapper.getTypeFactory().constructType(type);
            try {
                return objectMapper.readValue(json, javaType);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public <T> T decode(String json, TypeReference<T> valueType) {
            try {
                return objectMapper.readValue(json, valueType);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public JsonNode readTree(File file) throws IOException {
            try {
                return objectMapper.readTree(file);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class TestResource {
        @Id
        private int id;

        @Unique()
        private String name;

        private boolean married;

        private Params params1;
        private int[] params2;
    }

    public static class Params {
        private int xxx;

        public int getXxx() {
            return xxx;
        }

        public void setXxx(int xxx) {
            this.xxx = xxx;
        }
    }
}
