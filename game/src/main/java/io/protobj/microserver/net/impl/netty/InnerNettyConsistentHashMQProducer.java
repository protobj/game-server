package io.protobj.microserver.net.impl.netty;

import io.netty.channel.Channel;
import io.protobj.microserver.net.MQProtocol;
import io.protobj.microserver.net.impl.cluster.ClusterProducer;
import io.protobj.microserver.net.impl.cluster.ConsistentHashMQProducer;
import io.protobj.microserver.serverregistry.ServerInfo;

public class InnerNettyConsistentHashMQProducer extends ConsistentHashMQProducer {

    private InnerNettyConnector innerNettyTcpConnector;

    public void setInnerNettyTcpConnector(InnerNettyConnector innerNettyTcpConnector) {
        this.innerNettyTcpConnector = innerNettyTcpConnector;
    }

    @Override
    protected ClusterProducer newClusterProducer(ServerInfo newServerInfo) {
        InnerNettyClusterProducer innerNettyClusterProducer = new InnerNettyClusterProducer();
        innerNettyClusterProducer.setServerInfo(newServerInfo);
        innerNettyClusterProducer.setContext(getContext());
        Channel producer = innerNettyTcpConnector.createClientChannel(newServerInfo);
//TODO        NtceSvrRegister msg = new NtceSvrRegister(getContext().getSelfInfo().getFullSvrId(), newServerInfo.getFullSvrId());
//        byte[] encode = getContext().getSerilizer().encode(msg);
//        MQProtocol protocol = getContext().createProtocol(NtceSvrRegister.class.getSimpleName(), encode, 0, msg);
//        producer.writeAndFlush(protocol);
//        innerNettyClusterProducer.setChannel(producer);
        return innerNettyClusterProducer;
    }
}
