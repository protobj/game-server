package io.protobj.microserver.servicediscrovery;

import io.protobj.microserver.ServerType;
import io.protobj.microserver.serverregistry.ServerInfo;
import io.protobj.microserver.serverregistry.zk.ServerListener;
import org.apache.curator.x.discovery.ServiceCache;

import java.util.Collection;

public interface IServiceDiscovery {

    String SERVER_NAMESPACE = "svr";//监听服务的路径

    void start();

    void register(ServerInfo serverInfo);


    void update(ServerInfo serverInfo);

    void unregister(ServerInfo serverInfo);

    ServerInfo select(ServerInfo serverInfo, ServerType tgtType);

    ServerInfo query(ServerType ServerType, String id);

    void noteError(ServerInfo serverInfo);

    void close();

    void listenDestroy(String producerName, Runnable destroyCallback);

    ServiceCache<ServerInfo> newServiceCache(ServerType ServerType);

    void closeServerCache(ServerType type);
}
