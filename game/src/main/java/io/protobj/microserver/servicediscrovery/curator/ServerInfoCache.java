package io.protobj.microserver.servicediscrovery.curator;

import com.guangyu.cd003.projects.message.core.SvrType;
import com.guangyu.cd003.projects.message.core.serverregistry.ServerInfo;
import com.guangyu.cd003.projects.message.core.serverregistry.zk.ServerListener;
import com.guangyu.cd003.projects.message.core.servicediscrovery.IServiceDiscovery;
import com.guangyu.cd003.projects.microserver.MicroServer;
import com.pv.common.utilities.common.CommonUtil;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.x.discovery.ServiceCache;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.details.ServiceCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class ServerInfoCache implements ServiceCacheListener {

    private final Logger logger = LoggerFactory.getLogger(ServerInfoCache.class);

    private ServiceCache<ServerInfo> cache;

    private Map<Integer, ServerInfo> serviceInstanceMap = CommonUtil.createMap();

    private final ServerListener serverListener;

    private final SvrType svrType;

    public ServerInfoCache(SvrType type, IServiceDiscovery serviceDiscovery, ServerListener serverListener) {
        this.svrType = type;
        this.serverListener = serverListener;
        ServiceCache<ServerInfo> serverInfoServiceCache = serviceDiscovery.newServiceCache(type);
        serverInfoServiceCache.addListener(this);
        try {
            serverInfoServiceCache.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        this.cache = serverInfoServiceCache;
        List<ServiceInstance<ServerInfo>> instances = serverInfoServiceCache.getInstances();
        cacheChanged(instances);
        //关闭原有的服务器缓存列表
        serviceDiscovery.closeServerCache(type);
    }

    @Override
    public void cacheChanged() {
        List<ServiceInstance<ServerInfo>> instances = cache.getInstances();
        cacheChanged(instances);
    }

    @Override
    public void stateChanged(CuratorFramework client, ConnectionState newState) {

    }

    private void cacheChanged(Collection<ServiceInstance<ServerInfo>> instances) {
        Map<Integer, ServerInfo> serviceInstanceMap = CommonUtil.createMap();
        ServerListener serverListener = this.serverListener;
        for (ServiceInstance<ServerInfo> instance : instances) {
            ServerInfo serverInfo = instance.getPayload();
            this.serviceInstanceMap.remove(serverInfo.getServerId());
            if (serverListener != null) {
                serverListener.addOrUpdate(serverInfo);
            }
            serviceInstanceMap.put(serverInfo.getServerId(), serverInfo);
        }
        if (serverListener != null) {
            for (ServerInfo value : this.serviceInstanceMap.values()) {
                serverListener.remove(value);
            }
        }
        this.serviceInstanceMap = serviceInstanceMap;

    }

    public void close() {
        if (cache != null) {
            try {
                cache.close();
            } catch (IOException e) {
                logger.error("", e);
            }
        }
    }

    public ServerInfo getById(int id) {
        ServerInfo instance = serviceInstanceMap.get(id);
        if (instance != null) {
            return instance;
        }
        return null;
    }

    public ServerInfo selectByLoadRate() {
        return serviceInstanceMap
                .values()
                .stream()
                .min(Comparator.comparingInt(ServerInfo::getLoadRate))
                .get();
    }
}
