package io.protobj.services.transport.api;

import io.protobj.services.ServiceEndPoint;

public interface ClientTransport {

    ClientChannel create(ServiceEndPoint endPoint);
}
