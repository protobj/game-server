package io.protobj.microserver.net.impl.netty;

import com.guangyu.cd003.projects.message.common.msg.NtceSvrRegister;
import com.guangyu.cd003.projects.message.core.net.MQProtocol;
import com.guangyu.cd003.projects.message.core.net.impl.cluster.ClusterProducer;
import com.guangyu.cd003.projects.message.core.net.impl.cluster.ConsistentHashMQProducer;
import com.guangyu.cd003.projects.message.core.serverregistry.ServerInfo;
import io.netty.channel.Channel;

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
        NtceSvrRegister msg = new NtceSvrRegister(getContext().getSelfInfo().getFullSvrId(), newServerInfo.getFullSvrId());
        byte[] encode = getContext().getSerilizer().encode(msg);
        MQProtocol protocol = getContext().createProtocol(NtceSvrRegister.class.getSimpleName(), encode, 0, msg);
        producer.writeAndFlush(protocol);
        innerNettyClusterProducer.setChannel(producer);
        return innerNettyClusterProducer;
    }
}
