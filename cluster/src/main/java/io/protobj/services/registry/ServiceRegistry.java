package io.protobj.services.registry;

import io.protobj.services.ServiceInfo;

import java.util.List;

public interface ServiceRegistry {

    List<ServiceInfo> serviceInfos();

    List<ServiceInfo> lookupService(int type);

}
