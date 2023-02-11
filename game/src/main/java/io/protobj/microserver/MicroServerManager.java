package io.protobj.microserver;

import io.protobj.AServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务器容器
 * 服务器管理：开启、关闭、热更等功能
 */
public class MicroServerManager {

    private static final Logger logger = LoggerFactory.getLogger(MicroServerManager.class);

    public static final String BOOTSTRAP_CLASS = "bootstrapClass";
    public static final String MICRO_SERVER_SLOTS = "microServer.slots";//行会服务器槽位
    public static final String MICRO_SERVER_HOST = "microServer.host";//内部通信ip
    public static final String MICRO_SERVER_PORT = "microServer.port";//内部通信端口
    public static MicroServerManager singleton = new MicroServerManager();

    private MicroServerManager() {
    }

    private boolean init = false;

    private void init(Properties prop) {
        if (!init) {
            System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
            init = true;
        }
    }


    private final Map<String, Server> microServers = new ConcurrentHashMap<>();//开启的服务器

    public CompletableFuture<?> startServer(ServerType ServerType, int id, Properties properties) {
        try {
            long startTime = System.currentTimeMillis();
            Class<?> aClass = Class.forName(properties.getProperty(BOOTSTRAP_CLASS));
            Server microServer = (Server) aClass.getConstructor(ServerType.class, int.class).newInstance(ServerType, id);
            String key = ServerType.toFullSvrId(id);
            microServers.put(key, microServer);
            return microServer.start().thenRun(() -> {
                logger.warn("{} 启动耗时 : {}ms", key, System.currentTimeMillis() - startTime);
            }).exceptionally(e -> {
                logger.error("开启服务器失败", e);
                microServers.remove(key);
                return null;
            });
        } catch (Throwable e) {
            e.printStackTrace();
            CompletableFuture<AServer> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    public CompletableFuture<?> stopServer(ServerType ServerType, int id) {
        AServer microServer = microServers.get(ServerType.toFullSvrId(id));
        if (microServer != null) {
            return microServer.stop().thenApplyAsync(t -> {
                microServers.remove(ServerType.toFullSvrId(id));
                System.out.printf("关闭服务 %s%n", ServerType.toFullSvrId(id));
                return t;
            }).exceptionally(e -> {
                e.printStackTrace();
                System.out.printf("关闭服务失败 %s%n", ServerType.toFullSvrId(id));
                return null;
            });
        }
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<?> stopAll() {
        Collection<Server> values = microServers.values();
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (Server value : values) {
            CompletableFuture<?> future = stopServer(value.getServerType(), value.getSid());
            futures.add(future);
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    public void initAndStart(List<ServerConf> svrConfList, Properties prop) {
        init(prop);
        for (ServerConf svrConf : svrConfList) {
            ServerType ServerType = svrConf.getServerType();
            Properties properties = (Properties) prop.clone();
            if (svrConf.getSlots() != null) {
                properties.setProperty(MicroServerManager.MICRO_SERVER_SLOTS, svrConf.getSlots());
            }
//            properties.put(BOOTSTRAP_CLASS, ServerType.getBootstrapClass());
            startServer(ServerType, svrConf.getSvrId(), properties).join();
        }
    }
}
