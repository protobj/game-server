package io.protobj.microserver.net.impl.netty;

import io.netty.channel.Channel;
import io.protobj.microserver.loadbalance.SelectSvrStrategy;
import io.protobj.microserver.net.*;
import io.protobj.microserver.serverregistry.ServerInfo;
import io.protobj.microserver.servicediscrovery.IServiceDiscovery;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

public class NettyMQContext extends MQContext<MQProtocol> {

    private  static volatile Map<Integer, InnerNettyConnector> innerNettyConnectors = new ConcurrentHashMap<>();

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
    public MQProtocol createProtocol(String msgId, byte[] msg, int ix, Object crossSvrMsg) {

        return null;
    }

    @Override
    public void recv(String producerName, MQProtocol protocol) {
        recv(producerName, protocol.getMsgix(), protocol.getMsgId(), protocol.getMsgData(), protocol.getMsgKey());
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
        if (serverInfo.getServerType().getSelectSvrStrategy() == SelectSvrStrategy.ConsistentHash) {
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
//TODO        NtceSvrRegister msg = new NtceSvrRegister(selfInfo.getFullSvrId(), serverInfo.getFullSvrId());
//        byte[] encode = getSerilizer().encode(msg);
//        MQProtocol protocol = createProtocol(NtceSvrRegister.class.getSimpleName(), encode, 0, msg);
//        channel.writeAndFlush(protocol);
        return channel;
    }
}
