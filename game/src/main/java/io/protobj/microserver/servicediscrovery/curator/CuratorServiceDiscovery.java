package io.protobj.microserver.servicediscrovery.curator;

import com.guangyu.cd003.projects.message.core.SvrType;
import com.guangyu.cd003.projects.message.core.loadbalance.SelectSvrStrategy;
import com.guangyu.cd003.projects.message.core.serverregistry.ServerInfo;
import com.guangyu.cd003.projects.message.core.servicediscrovery.IServiceDiscovery;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.framework.recipes.cache.CuratorCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.x.discovery.*;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CuratorServiceDiscovery implements IServiceDiscovery {

    private static final Logger logger = LoggerFactory.getLogger(CuratorServiceDiscovery.class);


    private String namespace;

    private CuratorFramework curatorFramework;

    private ServiceDiscovery<ServerInfo> discovery;

    private EnumMap<SvrType, ServiceProvider<ServerInfo>> serverProvider = new EnumMap<>(SvrType.class);


    public CuratorServiceDiscovery(String connectString, String namespace) {
        this.namespace = namespace;
        curatorFramework = CuratorFrameworkFactory.builder()
                .connectString(connectString)
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .namespace(namespace)
                .waitForShutdownTimeoutMs(5000)
                .build();
        this.discovery = ServiceDiscoveryBuilder.builder(ServerInfo.class)
                .serializer(new JsonInstanceSerializer<>(ServerInfo.class))
                .basePath(SERVER_NAMESPACE)
                .client(curatorFramework)
                .build();
    }

    private ServiceInstance<ServerInfo> createServiceInstance(ServerInfo selfInfo) throws Exception {
        return ServiceInstance.<ServerInfo>builder()
                .payload(selfInfo)
                .serviceType(ServiceType.DYNAMIC)
                .registrationTimeUTC(System.currentTimeMillis())
                .enabled(true)
                .name(selfInfo.getSvrType().name())
                .id(String.valueOf(selfInfo.getServerId()))
                .build();
    }

    @Override
    public void start() {
        try {
            this.curatorFramework.start();
            this.curatorFramework.blockUntilConnected();

            this.discovery.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void register(ServerInfo serverInfo) {
        try {
            ServiceInstance<ServerInfo> serviceInstance = createServiceInstance(serverInfo);
            this.discovery.registerService(serviceInstance);
            Set<SvrType> follows = SvrType.getFollows(serverInfo.getSvrType());
            for (SvrType follow : follows) {
                //在provider里监听
                ServiceProvider<ServerInfo> serviceProvider = this.discovery.serviceProviderBuilder()
                        .executorService(null)
                        .serviceName(follow.name())
                        .providerStrategy(instanceProvider -> {
                            List<ServiceInstance<ServerInfo>> instances = instanceProvider.getInstances();
                            if (instances.isEmpty()) {
                                return null;
                            }

                            List<ServerInfo> collect = instances.stream().map(ServiceInstance::getPayload).collect(Collectors.toList());
                            ServerInfo select = follow.getSelectSvrStrategy().select(serverInfo, collect);
                            if (select == null) {
                                return null;
                            }

                            for (ServiceInstance<ServerInfo> instance : instances) {
                                if (instance.getPayload() == select) {
                                    return instance;
                                }
                            }
                            return null;
                        })
                        .build();
                serviceProvider.start();
                this.serverProvider.put(follow, serviceProvider);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void update(ServerInfo serverInfo) {
        try {
            this.discovery.updateService(createServiceInstance(serverInfo));
        } catch (Exception e) {
            logger.error("", e);
        }
    }

    @Override
    public void unregister(ServerInfo serverInfo) {
        try {
            this.discovery.unregisterService(createServiceInstance(serverInfo));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ServerInfo select(ServerInfo serverInfo, SvrType tgtType) {
        if (serverInfo.getSvrType() == tgtType && tgtType.getSelectSvrStrategy() == SelectSvrStrategy.ConsistentHash){
            return serverInfo;
        }
        ServiceProvider<ServerInfo> serverInfoServiceProvider = serverProvider.get(tgtType);
        try {
            ServiceInstance<ServerInfo> instance = serverInfoServiceProvider.getInstance();
            if (instance == null) {
                return null;
            }
            return instance.getPayload();
        } catch (Exception e) {
            logger.error("", e);
            return null;
        }
    }

    @Override
    public ServerInfo query(SvrType svrType, String id) {
        try {
            ServiceProvider<ServerInfo> serverInfoServiceProvider = serverProvider.get(svrType);
            if (serverInfoServiceProvider != null) {
                Collection<ServiceInstance<ServerInfo>> allInstances = serverInfoServiceProvider.getAllInstances();
                for (ServiceInstance<ServerInfo> allInstance : allInstances) {
                    if (allInstance.getId().equals(id)) {
                        return allInstance.getPayload();
                    }
                }
                return null;
            }
            ServiceInstance<ServerInfo> instance = discovery.queryForInstance(svrType.name(), id);
            if (instance == null) {
                return null;
            }
            return instance.getPayload();
        } catch (Exception e) {
            logger.error("", e);
            return null;
        }
    }

    @Override
    public void noteError(ServerInfo serverInfo) {
        ServiceProvider<ServerInfo> serverInfoServiceProvider = serverProvider.get(serverInfo.getSvrType());
        try {
            Collection<ServiceInstance<ServerInfo>> allInstances = serverInfoServiceProvider.getAllInstances();
            for (ServiceInstance<ServerInfo> allInstance : allInstances) {
                if (allInstance.getPayload().getServerId() == serverInfo.getServerId()) {
                    serverInfoServiceProvider.noteError(allInstance);
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("", e);
        }
    }

    @Override
    public void close() {
        for (ServiceProvider<ServerInfo> value : serverProvider.values()) {
            try {
                value.close();
            } catch (IOException e) {
                logger.error("", e);
            }
        }
        try {
            discovery.close();
        } catch (IOException e) {
            logger.error("", e);
        }
        curatorFramework.close();
    }

    @Override
    public void listenDestroy(String producerName, Runnable destroyCallback) {
        CuratorCache curatorCache = CuratorCache.builder(curatorFramework, "/"+SERVER_NAMESPACE + "/" + producerName)
                .withOptions(CuratorCache.Options.SINGLE_NODE_CACHE)
                .build();
        curatorCache.listenable().addListener((type, oldData, data) -> {
            if (type == CuratorCacheListener.Type.NODE_DELETED) {
                //服务器下线
                destroyCallback.run();
                curatorCache.close();
            }
        });
        curatorCache.start();
    }

    @Override
    public ServiceCache<ServerInfo> newServiceCache(SvrType svrType) {
        return discovery.serviceCacheBuilder()
                .name(svrType.name())
                .build();
    }

    @Override
    public void closeServerCache(SvrType type) {
        ServiceProvider<ServerInfo> remove = serverProvider.remove(type);
        if (remove != null) {
            try {
                remove.close();
            } catch (IOException e) {
                logger.error("", e);
            }
        }
    }

    public CuratorFramework getCuratorFramework() {
        return curatorFramework;
    }

    public ServiceDiscovery<ServerInfo> getDiscovery() {
        return discovery;
    }
}
