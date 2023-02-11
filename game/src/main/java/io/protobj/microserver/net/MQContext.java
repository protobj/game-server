package io.protobj.microserver.net;

import com.google.common.collect.Maps;
import com.guangyu.cd003.projects.message.common.msg.*;
import com.guangyu.cd003.projects.message.core.SvrType;
import com.guangyu.cd003.projects.message.core.loadbalance.SelectSvrStrategy;
import com.guangyu.cd003.projects.message.core.net.impl.MQSerilizerImpl;
import com.guangyu.cd003.projects.message.core.net.impl.cluster.ConsistentHashMQProducer;
import com.guangyu.cd003.projects.message.core.net.impl.cluster.JedisClusterCRC16;
import com.guangyu.cd003.projects.message.core.serverregistry.ServerInfo;
import com.guangyu.cd003.projects.message.core.servicediscrovery.IServiceDiscovery;
import com.guangyu.cd003.projects.microserver.log.ThreadLocalLoggerFactory;
import com.pv.common.utilities.common.CommonUtil;
import com.pv.common.utilities.exception.LogicException;
import com.pv.framework.gs.core.msg.CodeGameServerSys;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

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

    private final static Logger logger = ThreadLocalLoggerFactory.getLogger(MQContext.class);
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
    protected EnumMap<SvrType, MQProducer<T>> svrTypeProducerMap;
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
            throw new LogicException(CodeGameServerSys.UNKNOWN_SYS_EXCEPTION.getCode());
        });
    }

    public void start() throws Exception {
        producerMap = Maps.newHashMap();
        svrTypeProducerMap = Maps.newEnumMap(SvrType.class);
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
                svrTypeProducerMap.remove(value.getServerInfo().getSvrType());
            }
        }
        if (svrTypeProducerMap != null) {
            for (MQProducer<T> value : svrTypeProducerMap.values()) {
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

    public abstract T createProtocol(String msgId, byte[] msg, int ix, CrossSvrMsg crossSvrMsg);

    public abstract void recv(String producerName, T protocol);

    protected void recv(String producerName, int msgix, String msgId, byte[] msgData, long msgKey) {
        state.recv(this, producerName, msgix, msgId, msgData, msgKey);
    }

    protected abstract MQConsumer<T> newConsumer(ServerInfo serverInfo);

    protected abstract MQProducer<T> newProducer(ServerInfo serverInfo);

    public MQProducer<T> getOrCreateProducer(SvrType svrType, int id) {
        String fullSvrId = svrType.toFullSvrId(id);
        MQProducer<T> mqProducer1 = producerMap.get(fullSvrId);
        if (mqProducer1 != null) {
            return mqProducer1;
        }
        synchronized (this) {
            mqProducer1 = producerMap.get(fullSvrId);
            if (mqProducer1 != null) {
                return mqProducer1;
            }
            ServerInfo serverInfo = serviceDiscovery.query(svrType, String.valueOf(id));
            if (serverInfo == null) {
                throw new NetNotActiveException(svrType.toFullSvrId(id));
            }
            MQProducer<T> mqProducer = newProducer(serverInfo);
            producerMap.put(fullSvrId, mqProducer);
            if (serverInfo.getSvrType().getSelectSvrStrategy() == SelectSvrStrategy.RqstDns) {
                svrTypeProducerMap.put(serverInfo.getSvrType(), mqProducer);
            }
            return mqProducer;
        }
    }

    public MQProducer<T> getOrCreateProducer(SvrType svrType) {
        MQProducer<T> mqProducer1 = svrTypeProducerMap.get(svrType);
        if (mqProducer1 != null) {
            if (!mqProducer1.isClose()) {
                return mqProducer1;
            }
        }

        synchronized (this) {
            mqProducer1 = svrTypeProducerMap.get(svrType);
            if (mqProducer1 != null) {
                if (!mqProducer1.isClose()) {
                    return mqProducer1;
                }
            }
            MQProducer<T> mqProducer = null;
            ServerInfo serverInfo = null;
            while (true) {
                serverInfo = serviceDiscovery.select(selfInfo, svrType);
                if (serverInfo == null) {
                    break;
                }
                mqProducer1 = producerMap.get(serverInfo.getFullSvrId());
                if (mqProducer1 != null) {
                    if (!mqProducer1.isClose()) {
                        svrTypeProducerMap.put(svrType, mqProducer);
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
                throw new NetNotActiveException(svrType + " 不能连接");
            }
            String fullSrvId = serverInfo.getFullSvrId();
            producerMap.put(fullSrvId, mqProducer);
            svrTypeProducerMap.put(svrType, mqProducer);
            return mqProducer;
        }
    }

    public <A> CompletableFuture<List<A>> multiSend(SvrType targetType, List<Integer> sids, CrossSvrMsg msg, Class<A> answerClass) {
        ArrayList<Pair<Integer, CrossSvrMsg>> pairs = new ArrayList<>();
        for (Integer sid : sids) {
            pairs.add(Pair.of(sid, msg));
        }
        return multiSend(targetType, pairs, answerClass);
    }


    public <A> CompletableFuture<List<A>> multiSend(SvrType targetType, List<Pair<Integer, CrossSvrMsg>> msgs, Class<A> answerClass) {
        if (msgs.size() == 0) {
            throw new RuntimeException("发送数量为0");
        }
        if (msgs.size() == 1) {
            Pair<Integer, CrossSvrMsg> pair = msgs.get(0);
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
            for (Pair<Integer, CrossSvrMsg> pair : msgs) {
                MQProducer<T> producer = getOrCreateProducer(targetType, pair.getLeft());
                CrossSvrMsg msg = pair.getRight();
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
                    futureContainer.askCache.put(ix, Ask.createAsk(msg, future, producer.getServerInfo().getSvrType()));
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

    private Map<Integer, AtomicInteger> serverRegisterCount = CommonUtil.createMap();

    private void initLoadData(String producerName) {
        if (selfInfo.getSvrType().isLoadData()) {
            String[] split = producerName.split(SvrType.getSplitter());
            ServerInfo producerInfo = serviceDiscovery.query(SvrType.valueOf(split[0]), split[1]);
            AtomicInteger registerCount = serverRegisterCount.computeIfAbsent(producerInfo.getGroupId(), t -> new AtomicInteger());
            synchronized (registerCount) {
                int inc = registerCount.incrementAndGet();
                if (inc == 1) {
                    msgReceiver.load(selfInfo.getSvrType(), producerInfo.getGroupId());
                    state = State.Active;
                    selfInfo.addLoadRate();
                    serviceDiscovery.update(selfInfo);
                }
            }
            serviceDiscovery.listenDestroy(producerName, () -> {
                synchronized (registerCount) {
                    int decr = registerCount.decrementAndGet();
                    if (decr == 0) {
                        msgReceiver.unload(selfInfo.getSvrType(), producerInfo.getGroupId());
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
    public <A> CompletableFuture<A> ask(SvrType targetType, CrossSvrMsg msg, Class<A> ansClz) {
        if (targetType.getSelectSvrStrategy() == SelectSvrStrategy.RqstDns) {
            MQProducer<T> mqProducer = svrTypeProducerMap.get(targetType);
            if (mqProducer != null) {
                return send(mqProducer, msg, ansClz);
            }
            CompletableFuture<AnsAddress> addressFuture = ask(SvrType.DNS, new AskAddress(targetType, selfInfo.getGroupId()), AnsAddress.class);
            return addressFuture.thenComposeAsync(ansAddress -> {
                int serverId = ansAddress.getServerId();
                MQProducer<T> producer = getOrCreateProducer(targetType, serverId);
                return send(producer, msg, ansClz);
            });
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
    public <A> CompletableFuture<A> ask(SvrType targetType, int sid, CrossSvrMsg msg, Class<A> ansClz) {
        MQProducer<T> producer = getOrCreateProducer(targetType, sid);
        return send(producer, msg, ansClz);
    }


    private <A> CompletableFuture<A> send(MQProducer<T> producer, CrossSvrMsg msg, Class<A> answerClass) {
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
                futureContainer.askCache.put(ix, Ask.createAsk(msg, answerCompletableFuture, producer.getServerInfo().getSvrType()));
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
    public void answer(String producerName, int ix, CrossSvrMsg msg) {
        CompletableFuture.runAsync(() -> {
            byte[] encode = serilizer.encode(msg);
            String simpleName = msg.getClass().getSimpleName();
            String[] split = producerName.split(SvrType.getSplitter());
            MQProducer<T> producer = getOrCreateProducer(SvrType.valueOf(split[0]), Integer.parseInt(split[1]));
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

    public boolean isOnline(SvrType svrType, int sid) {
        return serviceDiscovery.query(svrType, String.valueOf(sid)) != null;
    }

    public void sendIfOnline(String toFullSvrId, NtceGameMsg sendMsg) {
        MQProducer<T> producer = producerMap.get(toFullSvrId);
        if (producer != null) {
            send(producer, sendMsg, null);
        }
    }

    public void removeProducerById(ServerInfo serverInfo) {
        producerMap.remove(serverInfo.getFullSvrId());
    }

    public synchronized void removeProducerByType(ServerInfo serverInfo) {
        MQProducer<T> producer = svrTypeProducerMap.get(serverInfo.getSvrType());
        if (producer == null) {
            return;
        }
        if (producer.getServerInfo().getServerId() == serverInfo.getServerId()) {
            svrTypeProducerMap.remove(serverInfo.getSvrType());
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
            public void recv(MQContext<?> context, String producerName, int msgix, String msgId, byte[] msgData, long msgKey) {
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
                                    MQProducer<MQProtocol> mqProducer = (MQProducer<MQProtocol>) context.svrTypeProducerMap.get(msgCallback.getAskSvrType());
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
                        if (selfInfo.getSvrType().getSelectSvrStrategy() == SelectSvrStrategy.ConsistentHash) {
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
                            ConsistentHashMQProducer orCreateProducer = (ConsistentHashMQProducer) context.getOrCreateProducer(selfInfo.getSvrType());
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
            }
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
