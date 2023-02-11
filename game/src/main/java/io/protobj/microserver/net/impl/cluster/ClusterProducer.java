package io.protobj.microserver.net.impl.cluster;

import com.guangyu.cd003.projects.message.core.net.MQProtocol;
import com.guangyu.cd003.projects.message.core.serverregistry.ServerInfo;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface ClusterProducer {
    CompletableFuture<?> sendAsync(MQProtocol msg);

    void close();

    void setServerInfo(ServerInfo serverInfo);

    ServerInfo getServerInfo();

    Set<VirtualNode> getNodes();
}
