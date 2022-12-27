package io.protobj;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import io.netty.channel.Channel;
import io.protobj.hotswap.HotSwapConfig;
import io.protobj.hotswap.HotSwapManger;
import io.protobj.network.gateway.NettyGateClient;
import io.protobj.network.gateway.NettyGateServer;
import io.protobj.resource.table.Id;
import io.protobj.resource.table.TableContainer;
import io.protobj.resource.table.Unique;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

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


        NettyGateServer nettyGateServer = new NettyGateServer(1);

        String localhost = "localhost";
        int port = 9999;
        CompletableFuture<Void> gate = nettyGateServer.startTcpBackendServer(localhost, port);
        gate.whenCompleteAsync((r, e) -> {
            if (e != null) {
                e.printStackTrace();
            } else {
                System.err.printf("gate backend[tcp] start in %s:%d%n", localhost, port);
            }
        }).join();

        NettyGateClient nettyGateClient = new NettyGateClient(1);
        CompletableFuture<Channel> client = nettyGateClient.startTcpBackendClient(localhost, port, 1);
        Channel join = client.join();
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
