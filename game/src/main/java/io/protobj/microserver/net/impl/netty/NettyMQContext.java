package io.protobj.microserver.net.impl.netty;

import com.google.protobuf.UnsafeByteOperations;
import com.guangyu.cd003.projects.message.common.msg.ConsistenceHashMsg;
import com.guangyu.cd003.projects.message.common.msg.CrossSvrMsg;
import com.guangyu.cd003.projects.message.common.msg.NtceSvrRegister;
import com.guangyu.cd003.projects.message.core.loadbalance.SelectSvrStrategy;
import com.guangyu.cd003.projects.message.core.net.*;
import com.guangyu.cd003.projects.message.core.serverregistry.ServerInfo;
import com.guangyu.cd003.projects.message.core.servicediscrovery.IServiceDiscovery;
import com.pv.common.utilities.common.CommonUtil;
import io.netty.channel.Channel;

import java.util.Map;
import java.util.concurrent.Executor;

public class NettyMQContext extends MQContext<MQProtocol> {

    private static volatile Map<Integer, InnerNettyConnector> innerNettyConnectors = CommonUtil.createMap();

    private InnerNettyConnector thisInnerNettyConnector;

    public NettyMQContext(ServerInfo selfInfo, MsgReceiver msgReceiver, IServiceDiscovery serviceDiscovery, Executor logicExecutor) {
        super(selfInfo, msgReceiver, serviceDiscovery, logicExecutor);
        thisInnerNettyConnector = innerNettyConnectors.computeIfAbsent(selfInfo.getPort(), k -> {
            String host = selfInfo.getHost();
            int port = selfInfo.getPort();
            InnerNettyConnector nettyTcpConnector = new InnerNettyTcpConnector(this.getSerilizer());
            nettyTcpConnector.init(host, port);
            return nettyTcpConnector;
        });
    }

    @Override
    public MQProtocol createProtocol(String msgId, byte[] msg, int ix, CrossSvrMsg crossSvrMsg) {
        MQProtocol.Builder builder = MQProtocol.newBuilder();
        builder.setMsgId(msgId);
        builder.setMsgData(UnsafeByteOperations.unsafeWrap(msg));
        builder.setMsgix(ix);
        if (crossSvrMsg instanceof ConsistenceHashMsg) {
            builder.setMsgKey(((ConsistenceHashMsg) crossSvrMsg).key());
        }
        MQProtocol build = builder.build();
        build.setAsk(crossSvrMsg);
        return build;
    }

    @Override
    public void recv(String producerName, MQProtocol protocol) {
        recv(producerName, protocol.getMsgix(), protocol.getMsgId(), protocol.getMsgData().toByteArray(), protocol.getMsgKey());
    }

    @Override
    protected MQConsumer<MQProtocol> newConsumer(ServerInfo serverInfo) {
        NettyConsumer mqProtocolNettyConsumer = new NettyConsumer();
        mqProtocolNettyConsumer.setMqContext(this);
        mqProtocolNettyConsumer.bindInnerTcpServer(thisInnerNettyConnector);
        return mqProtocolNettyConsumer;
    }

    @Override
    protected MQProducer<MQProtocol> newProducer(ServerInfo serverInfo) {
        if (serverInfo.getSvrType().getSelectSvrStrategy() == SelectSvrStrategy.ConsistentHash) {
            InnerNettyConsistentHashMQProducer mqProducer = new InnerNettyConsistentHashMQProducer();
            mqProducer.setServerInfo(serverInfo);
            mqProducer.setContext(this);
            mqProducer.setInnerNettyTcpConnector(thisInnerNettyConnector);
            mqProducer.create();
            return mqProducer;
        }
        NettyMQProducer nettyMQProducer = new NettyMQProducer();
        nettyMQProducer.setServerInfo(serverInfo);
        nettyMQProducer.setContext(this);
        Channel channel = createProducerChannel(serverInfo);
        nettyMQProducer.setChannel(channel);
        nettyMQProducer.listenDestroy();
        return nettyMQProducer;
    }

    Channel createProducerChannel(ServerInfo serverInfo) {
        Channel channel = thisInnerNettyConnector.createClientChannel(serverInfo);
        NtceSvrRegister msg = new NtceSvrRegister(selfInfo.getFullSvrId(), serverInfo.getFullSvrId());
        byte[] encode = getSerilizer().encode(msg);
        MQProtocol protocol = createProtocol(NtceSvrRegister.class.getSimpleName(), encode, 0, msg);
        channel.writeAndFlush(protocol);
        return channel;
    }
}
