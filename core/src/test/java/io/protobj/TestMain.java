package io.protobj;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.protobj.hotswap.HotSwapConfig;
import io.protobj.hotswap.HotSwapManger;

import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;

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


        Path path = Paths.get("C:\\Users\\79871\\GolandProjects\\excel-convert\\json\\server\\testresource_single.json");
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(path.toFile());
        JsonNode path1 = jsonNode.path("0.hp");
        System.err.println(path1.toString());
    }
}
