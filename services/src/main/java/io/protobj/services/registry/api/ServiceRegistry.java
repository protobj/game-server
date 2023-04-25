package io.protobj.services.registry.api;

import io.protobj.services.ServiceEndPoint;

import java.util.List;

public interface ServiceRegistry {

    void register(ServiceEndPoint endPoint);

    void update(ServiceEndPoint endPoint);

    void unregister(ServiceEndPoint endPoint);

    List<ServiceEndPoint> list();
}
