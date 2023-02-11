package io.protobj.microserver.net;

import com.google.common.collect.Maps;
import io.protobj.exception.LogicException;
import io.protobj.microserver.ServerType;
import io.protobj.microserver.loadbalance.SelectSvrStrategy;
import io.protobj.microserver.net.impl.MQSerilizerImpl;
import io.protobj.microserver.serverregistry.ServerInfo;
import io.protobj.microserver.servicediscrovery.IServiceDiscovery;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Created on 2021/6/29.
 * 提供跨服访问能力
 *
 * @author chen qiang
 */
public abstract class MQContext<T> {

    private final static Logger logger = LoggerFactory.getLogger(MQContext.class);
    private final MQMsgPrinter mqMsgPrinter = new MQMsgPrinter();
    protected ServerInfo selfInfo;
    /**
     * 消息处理器,数据加载卸载器
     */
    protected MsgReceiver msgReceiver;
    /**
     * 服务注册中心
     */
    protected IServiceDiscovery serviceDiscovery;
    /**
     * 监听自己的管道
     */
    protected MQConsumer<T> consumer;
    /**
     * 发送管道
     */
    protected Map<String, MQProducer<T>> producerMap;
    /**
     * 指定服务器类型发送消息
     */
    protected EnumMap<ServerType, MQProducer<T>> ServerTypeProducerMap;
    /**
     * 发送序列号
     */
    private final AtomicInteger msgIxGenerator = new AtomicInteger();
    /**
     * 请求消息future
     */
    private final FutureContainer futureContainer = new FutureContainer();
    /**
     * 消息序列化器
     */
    protected MQSerilizer serilizer = new MQSerilizerImpl();


    private Executor logicExecutor;

    private ExecutorService serilizerExecutor;


    protected State state = State.Init;

    public MQContext(ServerInfo selfInfo, MsgReceiver msgReceiver, IServiceDiscovery serviceDiscovery, Executor logicExecutor) {
        this.selfInfo = selfInfo.init();
        this.msgReceiver = msgReceiver;
        this.serviceDiscovery = serviceDiscovery;
        this.logicExecutor = logicExecutor;
        int poolSize = Runtime.getRuntime().availableProcessors() - 1;
        this.serilizerExecutor = new ThreadPoolExecutor(poolSize >> 2, poolSize,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(8192<<2), (r, executor) -> {
            logger.warn("线程池容量受限，检查异常或增大capacity ：{}",r);
            throw new LogicException(1);
        });
    }

    public void start() throws Exception {
        producerMap = Maps.newHashMap();
        ServerTypeProducerMap = Maps.newEnumMap(ServerType.class);
        this.consumer = newConsumer(selfInfo);
        serviceDiscovery.register(selfInfo);
    }


    public void close() {
        if (consumer != null) {
            this.consumer.close();
        }
        if (producerMap != null) {
            for (MQProducer<T> value : producerMap.values()) {
                value.close();
                ServerTypeProducerMap.remove(value.getServerInfo().getServerType());
            }
        }
        if (ServerTypeProducerMap != null) {
            for (MQProducer<T> value : ServerTypeProducerMap.values()) {
                value.close();
            }
        }
        if (serviceDiscovery != null) {
            serviceDiscovery.close();
        }

        if (serilizerExecutor != null) {
            serilizerExecutor.shutdown();
        }
    }

    public abstract T createProtocol(String msgId, byte[] msg, int ix, Object crossSvrMsg);

    public abstract void recv(String producerName, T protocol);

    protected void recv(String producerName, int msgix, String msgId, byte[] msgData, long msgKey) {
        state.recv(this, producerName, msgix, msgId, msgData, msgKey);
    }

    protected abstract MQConsumer<T> newConsumer(ServerInfo serverInfo);

    protected abstract MQProducer<T> newProducer(ServerInfo serverInfo);

    public MQProducer<T> getOrCreateProducer(ServerType ServerType, int id) {
        String fullSvrId = ServerType.toFullSvrId(id);
        MQProducer<T> mqProducer1 = producerMap.get(fullSvrId);
        if (mqProducer1 != null) {
            return mqProducer1;
        }
        synchronized (this) {
            mqProducer1 = producerMap.get(fullSvrId);
            if (mqProducer1 != null) {
                return mqProducer1;
            }
            ServerInfo serverInfo = serviceDiscovery.query(ServerType, String.valueOf(id));
            if (serverInfo == null) {
                throw new NetNotActiveException(ServerType.toFullSvrId(id));
            }
            MQProducer<T> mqProducer = newProducer(serverInfo);
            producerMap.put(fullSvrId, mqProducer);
            if (serverInfo.getServerType().getSelectSvrStrategy() == SelectSvrStrategy.RqstDns) {
                ServerTypeProducerMap.put(serverInfo.getServerType(), mqProducer);
            }
            return mqProducer;
        }
    }

    public MQProducer<T> getOrCreateProducer(ServerType ServerType) {
        MQProducer<T> mqProducer1 = ServerTypeProducerMap.get(ServerType);
        if (mqProducer1 != null) {
            if (!mqProducer1.isClose()) {
                return mqProducer1;
            }
        }

        synchronized (this) {
            mqProducer1 = ServerTypeProducerMap.get(ServerType);
            if (mqProducer1 != null) {
                if (!mqProducer1.isClose()) {
                    return mqProducer1;
                }
            }
            MQProducer<T> mqProducer = null;
            ServerInfo serverInfo = null;
            while (true) {
                serverInfo = serviceDiscovery.select(selfInfo, ServerType);
                if (serverInfo == null) {
                    break;
                }
                mqProducer1 = producerMap.get(serverInfo.getFullSvrId());
                if (mqProducer1 != null) {
                    if (!mqProducer1.isClose()) {
                        ServerTypeProducerMap.put(ServerType, mqProducer);
                        return mqProducer1;
                    }
                }
                mqProducer = newProducer(serverInfo);
                if (!mqProducer.isClose()) {
                    break;
                } else {
                    serviceDiscovery.noteError(serverInfo);
                }
            }
            if (serverInfo == null) {
                throw new NetNotActiveException(ServerType + " 不能连接");
            }
            String fullSrvId = serverInfo.getFullSvrId();
            producerMap.put(fullSrvId, mqProducer);
            ServerTypeProducerMap.put(ServerType, mqProducer);
            return mqProducer;
        }
    }

    public <A> CompletableFuture<List<A>> multiSend(ServerType targetType, List<Integer> sids, Object msg, Class<A> answerClass) {
        ArrayList<Pair<Integer, Object>> pairs = new ArrayList<>();
        for (Integer sid : sids) {
            pairs.add(Pair.of(sid, msg));
        }
        return multiSend(targetType, pairs, answerClass);
    }


    public <A> CompletableFuture<List<A>> multiSend(ServerType targetType, List<Pair<Integer, Object>> msgs, Class<A> answerClass) {
        if (msgs.size() == 0) {
            throw new RuntimeException("发送数量为0");
        }
        if (msgs.size() == 1) {
            Pair<Integer, Object> pair = msgs.get(0);
            CompletableFuture<A> ask = ask(targetType, pair.getLeft(), pair.getRight(), answerClass);
            CompletableFuture<List<A>> future = new CompletableFuture<>();
            if (answerClass == null) {
                future.complete(null);
                return future;
            }
            ask.thenAccept(t -> future.complete(Collections.singletonList(t)));
            return future;
        }
        CompletableFuture<List<A>> listCompletableFuture = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            List<CompletableFuture<A>> futures = new ArrayList<>();
            for (Pair<Integer, Object> pair : msgs) {
                MQProducer<T> producer = getOrCreateProducer(targetType, pair.getLeft());
                Object msg = pair.getRight();
                byte[] encode = serilizer.encode(msg);
                int ix = 0;
                if (answerClass != null) {
                    while ((ix = msgIxGenerator.incrementAndGet()) == 0) {
                    }
                }
                String simpleName = msg.getClass().getSimpleName();
                T protocol = createProtocol(simpleName, encode, ix, msg);

                CompletableFuture<A> future = new CompletableFuture<>();
                if (answerClass != null) {
                    futureContainer.askCache.put(ix, Ask.createAsk(msg, future, producer.getServerInfo().getServerType()));
                    futures.add(future);
                }
                int finalIx = ix;
                producer.sendAsync(protocol).exceptionally(e -> {
                    future.completeExceptionally(e.getCause());
                    futureContainer.askCache.invalidate(finalIx);
                    return null;
                });
            }

            if (answerClass != null) {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(
                        () -> listCompletableFuture.complete(futures.stream().map(CompletableFuture::join).collect(Collectors.toList()))
                ).exceptionally(
                        (err) -> {
                            listCompletableFuture.completeExceptionally(err);
                            return null;
                        }
                );
            } else {
                listCompletableFuture.complete(null);
            }
        }, serilizerExecutor).exceptionally(e -> {
            logger.error("multiSend", e.getCause());
            return null;
        });
        return listCompletableFuture;
    }

    public MQSerilizer getSerilizer() {
        return serilizer;
    }

    private Map<Integer, AtomicInteger> serverRegisterCount = new ConcurrentHashMap<>();

    private void initLoadData(String producerName) {
        if (selfInfo.getServerType().isLoadData()) {
            String[] split = producerName.split(ServerType.getSplitter());
            ServerInfo producerInfo = serviceDiscovery.query(ServerType.valueOf(split[0]), split[1]);
            AtomicInteger registerCount = serverRegisterCount.computeIfAbsent(producerInfo.getGroupId(), t -> new AtomicInteger());
            synchronized (registerCount) {
                int inc = registerCount.incrementAndGet();
                if (inc == 1) {
                    msgReceiver.load(selfInfo.getServerType(), producerInfo.getGroupId());
                    state = State.Active;
                    selfInfo.addLoadRate();
                    serviceDiscovery.update(selfInfo);
                }
            }
            serviceDiscovery.listenDestroy(producerName, () -> {
                synchronized (registerCount) {
                    int decr = registerCount.decrementAndGet();
                    if (decr == 0) {
                        msgReceiver.unload(selfInfo.getServerType(), producerInfo.getGroupId());
                        state = State.Init;
                        selfInfo.decLoadRate();
                        serviceDiscovery.update(selfInfo);
                    }
                }
            });
        } else {
            this.state = State.Active;
            selfInfo.addLoadRate();
            serviceDiscovery.update(selfInfo);
            serviceDiscovery.listenDestroy(producerName, () -> {
                state = State.Init;
                selfInfo.decLoadRate();
                serviceDiscovery.update(selfInfo);
            });
        }

    }

    /**
     * @param targetType 目标服务类型
     * @param msg        请求消息
     * @param ansClz     三种情况，指定class，表示对方要以这个class对象返回; Void.class 对方返回ansOk; null 通知消息，对方不需要返回数据
     */
    public <A> CompletableFuture<A> ask(ServerType targetType, Object msg, Class<A> ansClz) {
        if (targetType.getSelectSvrStrategy() == SelectSvrStrategy.RqstDns) {
            MQProducer<T> mqProducer = ServerTypeProducerMap.get(targetType);
            if (mqProducer != null) {
                return send(mqProducer, msg, ansClz);
            }
            //TODO
//            CompletableFuture<AnsAddress> addressFuture = ask(ServerType.DNS, new AskAddress(targetType, selfInfo.getGroupId()), AnsAddress.class);
//            return addressFuture.thenComposeAsync(ansAddress -> {
//                int serverId = ansAddress.getServerId();
//                MQProducer<T> producer = getOrCreateProducer(targetType, serverId);
//                return send(producer, msg, ansClz);
//            });
        }
        MQProducer<T> producer = null;
        try {
            producer = getOrCreateProducer(targetType);
        } catch (Exception e) {
            CompletableFuture<A> errorFuture = new CompletableFuture<>();
            errorFuture.completeExceptionally(e);
            return errorFuture;
        }
        return send(producer, msg, ansClz);
    }

    /**
     * @param targetType 目标服务类型
     * @param sid        指定服务id
     * @param msg        请求消息
     * @param ansClz     三种情况，指定class，表示对方要以这个class对象返回; Void.class 对方返回ansOk; null 通知消息，对方不需要返回数据
     */
    public <A> CompletableFuture<A> ask(ServerType targetType, int sid, Object msg, Class<A> ansClz) {
        MQProducer<T> producer = getOrCreateProducer(targetType, sid);
        return send(producer, msg, ansClz);
    }


    private <A> CompletableFuture<A> send(MQProducer<T> producer, Object msg, Class<A> answerClass) {
        CompletableFuture<A> answerCompletableFuture = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            int ix = 0;
            if (answerClass != null) {
                while ((ix = msgIxGenerator.incrementAndGet()) == 0) {
                }
            }
            byte[] decode = serilizer.encode(msg);
            String msgId = msg.getClass().getSimpleName();
            T protocol = createProtocol(msgId, decode, ix, msg);
            if (answerClass != null) {
                futureContainer.askCache.put(ix, Ask.createAsk(msg, answerCompletableFuture, producer.getServerInfo().getServerType()));
            }
            int finalIx = ix;
            producer.sendAsync(protocol).whenCompleteAsync((rs, err) -> {
                if (err != null) {
                    answerCompletableFuture.completeExceptionally(err);
                    futureContainer.askCache.invalidate(finalIx);
                } else if (answerClass == null) {
                    answerCompletableFuture.complete(null);
                }
            });
        }, serilizerExecutor).exceptionally(e -> {
            logger.error("send", e.getCause());
            answerCompletableFuture.completeExceptionally(e);
            return null;
        });
        return answerCompletableFuture;
    }

    /**
     * 返回消息
     */
    public void answer(String producerName, int ix, Object msg) {
        CompletableFuture.runAsync(() -> {
            byte[] encode = serilizer.encode(msg);
            String simpleName = msg.getClass().getSimpleName();
            String[] split = producerName.split(ServerType.getSplitter());
            MQProducer<T> producer = getOrCreateProducer(ServerType.valueOf(split[0]), Integer.parseInt(split[1]));
            T protocol = createProtocol(simpleName, encode, ix, msg);
            producer.sendAsync(protocol).exceptionally(e -> {
                logger.error("answer error", e);
                return null;
            });
        }, serilizerExecutor).exceptionally(e -> {
            logger.error("answer error", e);
            return null;
        });
    }

    public boolean isOnline(ServerType ServerType, int sid) {
        return serviceDiscovery.query(ServerType, String.valueOf(sid)) != null;
    }

    public void sendIfOnline(String toFullSvrId, Object sendMsg) {
        MQProducer<T> producer = producerMap.get(toFullSvrId);
        if (producer != null) {
            send(producer, sendMsg, null);
        }
    }

    public void removeProducerById(ServerInfo serverInfo) {
        producerMap.remove(serverInfo.getFullSvrId());
    }

    public synchronized void removeProducerByType(ServerInfo serverInfo) {
        MQProducer<T> producer = ServerTypeProducerMap.get(serverInfo.getServerType());
        if (producer == null) {
            return;
        }
        if (producer.getServerInfo().getServerId() == serverInfo.getServerId()) {
            ServerTypeProducerMap.remove(serverInfo.getServerType());
        }
    }

    public enum State {
        Init {
            @Override
            public void recv(MQContext<?> context, String producerName, int msgix, String msgId, byte[] msgData, long msgKey) {
                context.initLoadData(producerName);
                if (context.state != State.Active) {
                    logger.error("没有初始化");
                } else {
                    context.state.recv(context, producerName, msgix, msgId, msgData, msgKey);
                }
            }
        },
        Active {
            @Override
            public void recv(MQContext<?> context, String producerName, int msgix, String msgId, byte[] msgData, long msgKey) {/*
                String tId = context.selfInfo.getFullSvrId();
                try {
                    Ask<Object> msgCallback;
                    if (msgix != 0 && (msgCallback = context.futureContainer.askCache.getIfPresent(msgix)) != null) {
                        Object decode = context.serilizer.decode(msgId, msgData);
                        context.mqMsgPrinter.recvLog(producerName, tId, msgId, decode);
                        if (decode instanceof CodeMsg) {
                            int code = ((CodeMsg) decode).getCode();
                            if (code == 0) {
                                msgCallback.complete(null);
                            } else {
                                if (code == CodeGameServerSys.REDIRECT_EXCEPTION.getCode() && !msgCallback.isTimeout()) {
                                    //在超时时间内重试
                                    MQProducer<MQProtocol> mqProducer = (MQProducer<MQProtocol>) context.ServerTypeProducerMap.get(msgCallback.getAskServerType());
                                    if (mqProducer == null) {
                                        msgCallback.completeExceptionally(new LogicException(code));
                                    } else {
                                        Object ask = msgCallback.ask;
                                        MQProtocol protocol = (MQProtocol) context.createProtocol(ask.getClass().getSimpleName(), context.serilizer.encode(ask), msgix, (CrossSvrMsg) ask);
                                        mqProducer.sendAsync(protocol);
                                    }
                                    logger.warn("触发超时重试");
                                    return;
                                } else {
                                    msgCallback.completeExceptionally(new LogicException(code));
                                }
                            }
                        } else
                            msgCallback.complete(decode);//请求响应回调
                        context.futureContainer.askCache.invalidate(msgix);
                        msgCallback.recycle();
                    } else {
                        ServerInfo selfInfo = context.selfInfo;
                        if (selfInfo.getServerType().getSelectSvrStrategy() == SelectSvrStrategy.ConsistentHash) {
                            if (selfInfo.isMaintenance()) {
                                context.answer(producerName, msgix, new CodeMsg(CodeGameServerSys.UNKNOWN_SYS_EXCEPTION.getCode()));
                                return;
                            }
                            int slot = JedisClusterCRC16.getSlot(msgKey);
                            boolean proc = selfInfo.getSlotBits().get(slot);
                            if (proc) {
                                context.msgReceiver.recv(context, producerName, msgix, msgId, msgData);
                                return;
                            }
                            ConsistentHashMQProducer orCreateProducer = (ConsistentHashMQProducer) context.getOrCreateProducer(selfInfo.getServerType());
                            boolean selfProc = orCreateProducer.isSelfProc(slot);
                            if (selfProc) {
                                context.msgReceiver.recv(context, producerName, msgix, msgId, msgData);
                            } else {
                                context.answer(producerName, msgix, new CodeMsg(CodeGameServerSys.REDIRECT_EXCEPTION.getCode()));
                            }
                        } else {
                            context.msgReceiver.recv(context, producerName, msgix, msgId, msgData);//请求推送
                        }
                    }
                } catch (Exception e) {
                    logger.error("", e);
                }
            */}
        },
        ;

        public abstract void recv(MQContext<?> context, String producerName, int msgix, String msgId, byte[] msgData, long msgKey);
    }

    public ServerInfo getSelfInfo() {
        return selfInfo;
    }

    public IServiceDiscovery getServiceDiscovery() {
        return serviceDiscovery;
    }

    public MQMsgPrinter getMqMsgPrinter() {
        return mqMsgPrinter;
    }

    public Executor getLogicExecutor() {
        return logicExecutor;
    }
}
