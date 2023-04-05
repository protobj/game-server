package io.protobj.cluster.routing;

import io.protobj.cluster.ServiceInfo;
import io.protobj.cluster.registry.ServiceRegistry;

import java.util.Optional;

public interface Router {
    Optional<ServiceInfo> route(ServiceRegistry serviceRegistry, ServiceInfo selfServiceInfo);
}
