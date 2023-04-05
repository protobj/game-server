package io.protobj.cluster.registry;

import io.protobj.cluster.ServiceInfo;

import java.util.List;

public interface ServiceRegistry {

    List<ServiceInfo> serviceInfos();

    List<ServiceInfo> lookupService(int type);

}
