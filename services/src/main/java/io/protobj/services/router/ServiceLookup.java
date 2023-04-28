package io.protobj.services.router;

import io.protobj.services.ServiceEndPoint;
import reactor.core.publisher.Mono;

public interface ServiceLookup {
    Mono<ServiceEndPoint> lookupByGid(ServiceEndPoint local, int gid);

    Mono<ServiceEndPoint> lookupBySid(ServiceEndPoint local, int sid);

    Mono<ServiceEndPoint> lookup(ServiceEndPoint local);

    boolean focus(ServiceEndPoint endPoint);

    int st();

    void addOrUpd(ServiceEndPoint endPoint);

    void del(ServiceEndPoint endPoint);

}
