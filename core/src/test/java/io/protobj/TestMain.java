package io.protobj;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.Channel;
import io.protobj.enhance.EnhanceClassCache;
import io.protobj.event.EventBus;
import io.protobj.event.Test;
import io.protobj.hotswap.HotSwapConfig;
import io.protobj.hotswap.HotSwapManger;
import io.protobj.network.internal.NettyInternalClient;
import io.protobj.network.gateway.NettyGateServer;
import io.protobj.network.internal.session.MutilChannelSession;
import io.protobj.network.internal.session.Session;
import io.protobj.network.internal.session.SessionCache;
import io.protobj.redisaccessor.RedisAccessor;
import io.protobj.resource.ResourceManager;
import io.protobj.resource.table.Id;
import io.protobj.resource.table.TableContainer;
import io.protobj.resource.table.Unique;
import io.protobj.scheduler.HashedWheelTimer;
import io.protobj.scheduler.SchedulerService;
import io.protobj.user.UserModule;
import io.protobj.user.service.UserService;
import io.protobj.util.Jackson;

import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestMain implements IServer {

    public static final ThreadGroup TEST = new ThreadGroup("Test");

    public static void main(String[] args) throws Exception, IllegalAccessException, UnknownHostException {
//        testHotSwap();
//
//        testResource();
//
//        testNet();
//
//        testTimer();

        testScheduler();

        Thread.sleep(10000000);
    }

    private static void testScheduler() {
        TestMain testMain = new TestMain();
        testMain.schedulerService().init(List.of(new UserModule()), testMain);
    }

    private static void testHotSwap() {
        HotSwapManger hotSwapManger = new HotSwapManger(new HotSwapConfig("/home/chen/game/swap", "/home/chen/game/add", 8787));
        hotSwapManger.start();
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                new Test().print();

                Test test = new Test();
                test.testInner.print();

                Test.TestModule testModule = new Test.TestModule();
                testModule.print();
                System.err.println("------------------------");
            }
        }, 0, 2000);
    }

    private static void testResource() throws IOException {
        Path path = Paths.get("C:\\Users\\79871\\GolandProjects\\excel-convert\\json\\server\\testresource_single.json");
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(path.toFile());
        JsonNode path1 = jsonNode.path("0.hp");
        System.err.println(path1.toString());
        TableContainer<Integer, TestResource> tableContainer = new TableContainer<>(TestResource.class);
        Json jackSon = Jackson.INSTANCE;
        TableContainer<Integer, TestResource> load = tableContainer.load(Path.of("C:\\Users\\79871\\GolandProjects\\excel-convert\\json\\server\\testresource.json"), jackSon);
        System.err.println();
    }

    private static void testNet() {
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
        MutilChannelSession mutilChannelSession = new MutilChannelSession();
        NettyInternalClient nettyInternalClient = new NettyInternalClient(1, new SessionCache() {
            @Override
            public Session getSessionById(int channelId) {
                return null;
            }
        }, null, null);
        for (int i = 0; i < 3; i++) {
            CompletableFuture<Channel> client = nettyInternalClient.start(localhost, port, 1);
            Channel join = client.join();
            mutilChannelSession.add(join);
        }
    }

    private static void testTimer() {
        HashedWheelTimer hashedWheelTimer = new HashedWheelTimer(new ThreadGroup("Test-Timer"), 1000, 60, null);

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
    }

    UserService userService = new UserService();

    @Override
    public <T> T getBeanByType(Class<T> clazz) {
        return (T) userService;
    }

    EnhanceClassCache enhanceClassCache = new EnhanceClassCache();

    @Override
    public EnhanceClassCache getEnhanceClassCache() {

        return enhanceClassCache;
    }

    @Override
    public EventBus getEventBus() {
        return null;
    }

    @Override
    public ResourceManager getResourceManager() {
        return null;
    }

    @Override
    public RedisAccessor getRedisAccessor() {
        return null;
    }

    @Override
    public HotSwapManger getHotSwapManger() {
        return null;
    }


    ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    public Executor getManageExecutor() {

        return executorService;
    }

    @Override
    public Executor getLogicExecutor() {
        return null;
    }

    @Override
    public ThreadGroup threadGroup() {
        return TEST;
    }

    SchedulerService schedulerService = new SchedulerService();

    @Override
    public SchedulerService schedulerService() {

        return schedulerService;
    }

    @Override
    public SessionManager sessionManager() {
        return null;
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
