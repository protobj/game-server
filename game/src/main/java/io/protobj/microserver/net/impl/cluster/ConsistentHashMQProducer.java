package io.protobj.microserver.net.impl.cluster;

import com.guangyu.cd003.projects.message.core.net.MQProducer;
import com.guangyu.cd003.projects.message.core.net.MQProtocol;
import com.guangyu.cd003.projects.message.core.net.NetNotActiveException;
import com.guangyu.cd003.projects.message.core.serverregistry.ServerInfo;
import com.guangyu.cd003.projects.message.core.serverregistry.zk.ServerListener;
import com.guangyu.cd003.projects.message.core.servicediscrovery.curator.ServerInfoCache;
import com.guangyu.cd003.projects.microserver.log.ThreadLocalLoggerFactory;
import com.pv.common.utilities.common.GsonUtil;
import com.pv.common.utilities.exception.LogicException;
import com.pv.framework.gs.core.msg.CodeGameServerSys;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Supplier;

/**
 * 假设几种情况
 * <p>
 * 当有服务器挂掉，如何恢复
 * 假设有1000个槽位，有4台行会服务器已经分配好了slot
 * 分别为 1：0-249，
 * 2：250-499，
 * 3：500-749，
 * 4：750-999
 * 1.恢复重启：此时2服挂了，2服的服务有3服来处理，将2服重启，先将3服变为维护状态，启动2服后将3服修改会正常状态
 * 2.在线扩容：假设一服的压力很大，槽位需要重新分配，将1服设置为维护状态。设置槽位为0-124，启动5服设置槽位125-249，最后设置1服状态为正常
 * 3.在线缩容：与扩容类似 设置对应服务器为维护状态，将一台服务器设置槽位为空，将另一台设置为两台之和，再开启
 **/
public abstract class ConsistentHashMQProducer extends MQProducer<MQProtocol> implements ServerListener {
    public static final int MAX_SLOT = 16384;
    private final static Logger logger = ThreadLocalLoggerFactory.getLogger(ConsistentHashMQProducer.class);

    private final StampedLock lock = new StampedLock();

    //集群节点数据
    private final Int2ObjectMap<ClusterProducer> producerMap = new Int2ObjectOpenHashMap<>();
    //hash槽
    private final VirtualNode[] slots = new VirtualNode[MAX_SLOT];
    //判断集群是否填充满
    private final BitSet slotFlag = new BitSet(MAX_SLOT);
    // 通过startSlot排序，方便找出下一个
    private SortedArrayList<VirtualNode> virtualNodes;

    private ServerInfoCache serverInfoCache;

    public ConsistentHashMQProducer() {
        virtualNodes = new SortedArrayList<>(Comparator.comparing(VirtualNode::getStartSlot));
    }

    private VirtualNode testNode = new VirtualNode(0, 0, 0);

    public boolean isSelfProc(int slot) {
        long stamp = lock.tryOptimisticRead();
        VirtualNode virtualNode = getVirtualNode(slot);
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                virtualNode = getVirtualNode(slot);
                return virtualNode.getServerId() == getServerInfo().getServerId();
            } finally {
                lock.unlockRead(stamp);
            }
        } else {
            return virtualNode.getServerId() == getServerInfo().getServerId();
        }
    }

    @Override
    public CompletableFuture<?> sendAsync(MQProtocol msg) {
        long key = msg.getMsgKey();
        //拿到slot
        if (key == 0) {
            //直接找一台可用的服务器
            throw new RuntimeException("key 为 空 " + msg.getMsgId());
        }
        int slot = JedisClusterCRC16.getSlot(key);

        ClusterProducer clusterProducer = runWithReadLock(() -> {
            VirtualNode virtualNode = getVirtualNode(slot);
            ClusterProducer clusterProducer0 = producerMap.get(virtualNode.getServerId());
            //服务器维护,不接受请求
            if (clusterProducer0.getServerInfo().isMaintenance()) {
                throw new NetNotActiveException(clusterProducer0.getServerInfo().getFullSvrId());
            }
            return clusterProducer0;
        });
//        getContext().getMqMsgPrinter().sendLog(getContext().getSelfInfo().getFullSvrId(), clusterProducer.getServerInfo().getFullSvrId(), msg.getMsgId(), msg.getAsk());
        return clusterProducer.sendAsync(msg);
    }

    private <T> T runWithReadLock(Supplier<T> supplier) {
        return runWithReadLock(lock, supplier);
    }

    public static <T> T runWithReadLock(StampedLock lock, Supplier<T> supplier) {
        long stamp = lock.tryOptimisticRead();
        T t = supplier.get();
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                return supplier.get();
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return t;
    }
    private VirtualNode getVirtualNode(int slot) {
        VirtualNode virtualNode = slots[slot];
        if (virtualNode == null) {
            testNode.setStartSlot(slot);
            int insertionPoint = virtualNodes.findInsertionPoint(testNode);
            //选服成功
            if (insertionPoint >= virtualNodes.size()) {
                if (slot <= virtualNodes.get(virtualNodes.size() - 1).getEndSlot()) {
                    virtualNode = virtualNodes.get(virtualNodes.size() - 1);
                } else {
                    virtualNode = virtualNodes.get(0);
                }
            } else {
                VirtualNode virtualNode1 = virtualNodes.get(insertionPoint - 1);
                if (slot <= virtualNode1.getEndSlot()) {
                    virtualNode = virtualNodes.get(insertionPoint - 1);
                } else {
                    virtualNode = virtualNodes.get(insertionPoint);
                }
            }
        }
        return virtualNode;
    }

    @Override
    public void close() {
        if (serverInfoCache != null) {
            serverInfoCache.close();
            serverInfoCache = null;
        }
    }

    @Override
    public void listenDestroy() {

    }

    @Override
    public boolean isClose() {
        return false;
    }

    public void create() {
        //集群服务器需要监听每台服务器的状态
        this.serverInfoCache = new ServerInfoCache(getServerInfo().getSvrType(), getContext().getServiceDiscovery(), this);
    }


    @Override
    public void addOrUpdate(ServerInfo serverInfo) {
        if (serverInfo.getSvrType() != getServerInfo().getSvrType()) {
            return;
        }
        //检查slots合法性
        List<Integer> slots = serverInfo.getSlots();
        for (int i = 0; i < slots.size(); i += 2) {
            int start = slots.get(i);
            int end = slots.get(i + 1);
            if (start > end || start < 0 || end >= MAX_SLOT) {
                logger.info("slots配置：{},{} {}", serverInfo.getFullSvrId(), start, end);
                return;
            }
        }
        //初始化
        StampedLock stampedLock = this.lock;
        long lock = stampedLock.writeLock();
        try {
            stateInitAddOrUpdate(serverInfo);
        } finally {
            stampedLock.unlockWrite(lock);
        }
    }

    private void stateInitRemove(ServerInfo serverInfo) {
        ClusterProducer clusterProducer = producerMap.get(serverInfo.getServerId());
        if (clusterProducer == null) {
            return;
        }
        clearSlots(clusterProducer.getServerInfo().getSlots());
        closeProducer(clusterProducer);
        logger.info("stateInitRemove ：{}", GsonUtil.toJSONString(clusterProducer.getServerInfo()));
    }

    private void stateInitAddOrUpdate(ServerInfo newServerInfo) {
        logger.info("stateInitAddOrUpdate {}", GsonUtil.toJSONString(newServerInfo));
        ClusterProducer clusterProducer = producerMap.get(newServerInfo.getServerId());
        BitSet slotBits = newServerInfo.getSlotBits();
        //已加入过
        if (clusterProducer != null) {
            ServerInfo oldServerInfo = clusterProducer.getServerInfo();
            logger.info("加入的节点已存在，原来的状态：{}", GsonUtil.toJSONString(oldServerInfo));
            if (serverInfoSlotChange(newServerInfo, oldServerInfo)) {
                //排除自身所占，查询槽点是否已被占用
                BitSet bitSet = BitSet.valueOf(this.slotFlag.toLongArray());
                bitSet.andNot(oldServerInfo.getSlotBits());
                if (slotBits.intersects(bitSet)) {
                    logger.info("配置错误，slots重复");
                    throw new RuntimeException("一些槽点已经被占用");
                }
                List<Integer> slots = oldServerInfo.getSlots();
                clearSlots(slots);
                virtualNodes.removeAll(clusterProducer.getNodes());
                clusterProducer.getNodes().clear();
                if (newServerInfo.getSlots().isEmpty()) {
                    closeProducer(clusterProducer);
                    logger.info("新状态没有slot，删除节点缓存");
                    return;
                }
                setNewVirtualNode(newServerInfo, clusterProducer);
            }
            clusterProducer.setServerInfo(newServerInfo);
        } else {
            if (newServerInfo.getSlots().isEmpty()) {
                logger.info("加入的节点不存在，但是没有slots,加入失败");
                return;
            }
            logger.info("设置新的slot");
            boolean intersects = slotFlag.intersects(slotBits);
            if (intersects) {
                logger.info("配置错误，slots重复");
                throw new RuntimeException("一些槽点已经被占用");
            }
            //不设置
            try {
                clusterProducer = newClusterProducer(newServerInfo);
                clusterProducer.setServerInfo(newServerInfo);
                producerMap.put(newServerInfo.getServerId(), clusterProducer);
                setNewVirtualNode(newServerInfo, clusterProducer);
                if (slotFlag.cardinality() == MAX_SLOT) {
                    logger.info("hash slots full");
                }
            } catch (NetNotActiveException e) {
                logger.error("", e);
            }
        }
    }

    protected abstract ClusterProducer newClusterProducer(ServerInfo newServerInfo);

    private void setNewVirtualNode(ServerInfo newServerInfo, ClusterProducer clusterProducer) {
        slotFlag.or(newServerInfo.getSlotBits());
        List<Integer> slots = newServerInfo.getSlots();
        for (int i = 0; i < slots.size(); i += 2) {
            int start = slots.get(i);
            int end = slots.get(i + 1);
            VirtualNode virtualNode = new VirtualNode(start, end, newServerInfo.getServerId());
            clusterProducer.getNodes().add(virtualNode);
            virtualNodes.add(virtualNode);
            Arrays.fill(this.slots, start, end + 1, virtualNode);
        }
    }

    private void closeProducer(ClusterProducer clusterProducer) {
        producerMap.remove(clusterProducer.getServerInfo().getServerId());
        virtualNodes.removeAll(clusterProducer.getNodes());
        clusterProducer.close();
    }

    private void clearSlots(List<Integer> slots) {
        for (int i = 0; i < slots.size(); i += 2) {
            int start = slots.get(i);
            int end = slots.get(i + 1);
            //清空
            Arrays.fill(this.slots, start, end + 1, null);
            slotFlag.clear(start, end + 1);
        }
    }

    private static boolean serverInfoSlotChange(ServerInfo serverInfo, ServerInfo serverInfo1) {
        return !serverInfo.getSlots().equals(serverInfo1.getSlots());
    }

    private static boolean serverInfoStateChange(ServerInfo serverInfo, ServerInfo serverInfo1) {
        return serverInfo.getState() != serverInfo1.getState();
    }

    @Override
    public void remove(ServerInfo serverInfo) {
        if (serverInfo.getSvrType() != getServerInfo().getSvrType()) {
            return;
        }
        StampedLock stampedLock = this.lock;
        long lock = stampedLock.writeLock();
        try {
            stateInitRemove(serverInfo);
        } finally {
            stampedLock.unlockWrite(lock);
        }
    }
}
