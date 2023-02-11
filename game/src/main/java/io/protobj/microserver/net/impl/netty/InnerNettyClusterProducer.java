package io.protobj.microserver.net.impl.netty;

import com.guangyu.cd003.projects.message.core.net.impl.cluster.ClusterProducer;
import com.guangyu.cd003.projects.message.core.net.impl.cluster.VirtualNode;

import java.util.HashSet;
import java.util.Set;

public class InnerNettyClusterProducer extends NettyMQProducer implements ClusterProducer {
    private Set<VirtualNode> nodes = new HashSet<VirtualNode>();

    @Override
    public Set<VirtualNode> getNodes() {
        return nodes;
    }
}
