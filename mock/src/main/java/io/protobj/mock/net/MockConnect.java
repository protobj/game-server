package io.protobj.mock.net;


import io.netty.channel.Channel;
import io.netty.channel.DefaultChannelPromise;
import io.protobj.mock.MockContext;
import io.protobj.mock.plan.Plan;
import io.protobj.util.Jackson;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Supplier;

import static io.protobj.mock.MockContext.statisticsInfo;

/**
 * Created on 2021/5/20.
 *
 * @author chen qiang
 */
public class MockConnect {

    //    private final static Logger logger = LoggerFactory.getLogger(MockConnect.class);
    Logger logger = getLogger();

    private String uid;
    private String serverId;
    private Channel channel;

    public MockContext mockContext;

    /***************************************游戏内数据START***************************************************/
    public long sysTime;

    //上一次收到的消息
    public Map<Integer, Object> LAST_RECV_MSGS = new ConcurrentHashMap<>();


    /***************************************游戏内数据END***************************************************/


    public Map<Integer, List<RqstFuture>> rqstFutureMap = new ConcurrentHashMap<>();

    public Supplier<? extends Plan> planSupplier;

    private static final Map<ScheduledExecutorService, Scheduler> schedulerMap = new ConcurrentHashMap<>();

    private ScheduledExecutorService executor;
    private Scheduler scheduler;

    private ScheduledFuture<?> heartbeatFuture;

    private boolean planEnd;

    public MockConnect(MockContext mockContext, String uid, String serverId, Supplier<? extends Plan> planSupplier) {
        this.mockContext = mockContext;
        this.uid = uid;
        this.serverId = serverId;
        this.planSupplier = planSupplier;
    }

    public String getUid() {
        return uid;
    }

    public void setChannel(Channel channel, ScheduledExecutorService executor) {
        this.channel = channel;
        this.executor = executor;
        scheduler = schedulerMap.computeIfAbsent(executor, t -> RxJavaPlugins.createExecutorScheduler(t, false, false));
        heartbeatFuture = executor.scheduleAtFixedRate(() -> {
            if (channel != null && channel.isActive()) {
                //todo sendHeartbeat(channel);
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public Channel getChannel() {
        return channel;
    }

    public RqstFuture send(int cmd, Object sendBytes) {
        String name = Thread.currentThread().getName();
        if (!name.contains("MockExecutorGroup")) {
            Thread.dumpStack();
            throw new UnsupportedOperationException("没有在线程中执行发送逻辑");
        }
        RqstFuture rqstFuture = new RqstFuture(cmd);
        try {
            List<RqstFuture> before = rqstFutureMap.computeIfAbsent(cmd, t -> new CopyOnWriteArrayList<>());
            if (before.size() > 0) {
                Iterator<RqstFuture> iterator = before.iterator();
                while (iterator.hasNext()) {
                    RqstFuture beforeRqst = iterator.next();
                    if (beforeRqst.rqstTime != 0 && beforeRqst.rqstTime + TimeUnit.MINUTES.toMillis(5) < System.currentTimeMillis()) {
                        if (cmd != 511 && cmd != 512 && cmd != 304) {
                            logger.error("请求超时：{} {} {}", beforeRqst.rqstTime, cmd, ExceptionUtils.getStackTrace(new Exception()));
                            beforeRqst.completeExceptionally(new CodeException(cmd, 1));
                        } else {
                            beforeRqst.completeExceptionally(new CodeException(cmd, 1));
                        }
                        iterator.remove();
                    } else {
//                    throw new UnsupportedOperationException("不支持同时发送两个相同cmd请求 " + cmd);
                    }
                }
            }
            CompletableFuture<Void> encodeFuture = null;
            if (sendBytes == null) {
                //TODO encodeFuture = req(channel, cmd);
            } else {
                //TODO  encodeFuture = req(channel, cmd, sendBytes);
            }
            before.add(rqstFuture);
            encodeFuture.thenRun(() -> {
                rqstFuture.setRqstTime(System.currentTimeMillis());
                statisticsInfo.rqst(cmd);
            }).exceptionally(e -> {
                rqstFuture.completeExceptionally(e);
                return null;
            });
            if (mockContext.config.isPrintLog(cmd)) {
                if (sendBytes == null) {
                    logger.info("mock send:{}", cmd);
                } else {
                    logger.info("mock send:{} {}:{}", cmd, sendBytes.getClass().getSimpleName(), Jackson.INSTANCE.encode(sendBytes));
                }
            }
        } catch (UnsupportedOperationException e) {
            rqstFuture.completeExceptionally(e);
        }

        return rqstFuture;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }


    public CompletableFuture<Integer> tryClose() {
        DefaultChannelPromise promise = new DefaultChannelPromise(channel);
        CompletableFuture<Integer> closeFuture = new CompletableFuture<>();
        channel.close(promise).addListener(future -> {
            logger.info("login out!! {}", uid);
            mockContext.removeConnect(channel);
            close();
            closeFuture.complete(0);
        });
        return closeFuture;
    }

    public void close() {
        if (heartbeatFuture != null) {
            heartbeatFuture.cancel(true);
        }
    }

    public Logger getLogger() {
        if (this.logger == null) {
            this.logger = LoggerFactory.getLogger(MockConnect.class);
        }
        return this.logger;
    }


    public void handleData(int cmd, Object result) {

    }

    public ScheduledExecutorService executor() {
        return executor;
    }


    public boolean isSending(int rqstCmd) {
//        List<RqstFuture> rqstFutures = rqstFutureMap.get(rqstCmd);
//        return CollectionUtils.isNotEmpty(rqstFutures);
        return false;
    }

    public boolean isOffLine() {
        return channel == null || !channel.isActive();
    }

    public void setPlanEnd() {
        this.planEnd = true;
    }

    public boolean isPlanEnd() {
        return planEnd;
    }
}
