package io.protobj.microserver.servicediscrovery;

import com.guangyu.cd003.projects.message.core.SvrType;
import com.guangyu.cd003.projects.message.core.serverregistry.ServerInfo;
import com.guangyu.cd003.projects.message.core.serverregistry.zk.ServerListener;
import org.apache.curator.x.discovery.ServiceCache;

import java.util.Collection;

public interface IServiceDiscovery {

    String SERVER_NAMESPACE = "svr";//监听服务的路径

    void start();

    void register(ServerInfo serverInfo);


    void update(ServerInfo serverInfo);

    void unregister(ServerInfo serverInfo);

    ServerInfo select(ServerInfo serverInfo, SvrType tgtType);

    ServerInfo query(SvrType svrType, String id);

    void noteError(ServerInfo serverInfo);

    void close();

    void listenDestroy(String producerName, Runnable destroyCallback);

    ServiceCache<ServerInfo> newServiceCache(SvrType svrType);

    void closeServerCache(SvrType type);
}
