package io.protobj.cluster;

import io.protobj.cluster.discovery.ServiceDiscovery;
import io.protobj.cluster.message.MessageHandler;
import io.protobj.cluster.registry.ServiceRegistry;
import io.protobj.cluster.transport.Sender;
import io.scalecube.transport.netty.Receiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.Executor;

public class ClusterContext {
    private final static Logger logger = LoggerFactory.getLogger(ClusterContext.class);

    private ClusterMember selfMember;

    private ServiceRegistry serviceRegistry;

    private ServiceDiscovery serviceDiscovery;

    private MessageHandler messageHandler;

    private Receiver receiver;

    private Map<Integer, Sender> sender;

    private Executor executor;


    private Receiver newReceiver() {
        return null;
    }

    public ServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }

    public void setServiceRegistry(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    public ServiceDiscovery getServiceDiscovery() {
        return serviceDiscovery;
    }

    public void setServiceDiscovery(ServiceDiscovery serviceDiscovery) {
        this.serviceDiscovery = serviceDiscovery;
    }
}
