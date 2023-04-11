package io.protobj.services.routing;

import io.protobj.services.ServiceInfo;
import io.protobj.services.registry.ServiceRegistry;

import java.util.Optional;

public interface Router {
    Optional<ServiceInfo> route(ServiceRegistry serviceRegistry, ServiceInfo selfServiceInfo);
}
