package io.protobj.microserver.net.impl.cluster;


import io.protobj.microserver.net.MQProtocol;
import io.protobj.microserver.serverregistry.ServerInfo;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface ClusterProducer {
    CompletableFuture<?> sendAsync(MQProtocol msg);

    void close();

    void setServerInfo(ServerInfo serverInfo);

    ServerInfo getServerInfo();

    Set<VirtualNode> getNodes();
}
