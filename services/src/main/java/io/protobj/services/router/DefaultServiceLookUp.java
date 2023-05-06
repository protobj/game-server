package io.protobj.services.router;

import io.protobj.services.ServiceContext;
import io.protobj.services.ServiceEndPoint;
import io.protobj.services.transport.api.ClientChannel;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.Comparator;
import java.util.stream.Collectors;

public class DefaultServiceLookUp implements ServiceLookup {
    private final Int2ObjectMap<ServiceEndPoint> serviceMap = new Int2ObjectOpenHashMap<>();

    @Override
    public ClientChannel lookup(ServiceContext serviceContext, ServiceEndPoint local, LookupParam param) {
        if (param.getType() == LookupParam.MIN) {
            ServiceEndPoint data =  serviceMap.values().stream().sorted(Comparator.comparing(ServiceEndPoint::getSid)).collect(Collectors.toList()).get(0);
            if (data != null) {
                ServiceContext.ServiceTransportBootstrap transportBootstrap = serviceContext.getTransportBootstrap();
                return transportBootstrap.getClientTransport().create(data);
            }
        }
        return null;
    }

    @Override
    public boolean focus(ServiceEndPoint endPoint) {
        return endPoint.getSt() == st();
    }

    @Override
    public int st() {
        return 0;
    }

    @Override
    public void addOrUpd(ServiceEndPoint endPoint) {
        serviceMap.put(endPoint.getSid(), endPoint);

    }

    @Override
    public void del(ServiceEndPoint endPoint) {
        serviceMap.remove(endPoint.getSid());
    }
}
