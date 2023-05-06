package io.protobj.services.router;

import io.protobj.services.ServiceContext;
import io.protobj.services.ServiceEndPoint;
import io.protobj.services.transport.api.ClientChannel;
import reactor.core.publisher.Mono;

public interface ServiceLookup {
    ClientChannel lookup(ServiceContext serviceContext, ServiceEndPoint local, LookupParam param);

    boolean focus(ServiceEndPoint endPoint);

    int st();

    void addOrUpd(ServiceEndPoint endPoint);

    void del(ServiceEndPoint endPoint);

}
