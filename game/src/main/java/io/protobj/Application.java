package io.protobj;

import io.protobj.hotswap.HotSwapConfig;
import io.protobj.redisaccessor.config.RedisConfig;
import io.protobj.resource.ResourceConfig;
import io.protobj.util.Jackson;

public class Application {
    public static void main(String[] args) {
        //异步分线程写日志
//        System.setProperty("log4j2.contextSelector", "org.apache.logging.log4j.core.async.CustomAsyncLoggerContextSelector");

    }
}