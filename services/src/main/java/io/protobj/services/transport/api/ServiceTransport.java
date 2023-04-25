package io.protobj.services.transport.api;

import io.protobj.services.ServiceEndPoint;
import reactor.core.publisher.Mono;

public interface ServiceTransport {

    ClientTransport clientTransport();
    ServerTransport serverTransport(ServiceEndPoint localEndPoint);

    Mono<? extends ServiceTransport> start();

    /**
     * Shutdowns transport.
     *
     * @return shutdown completion signal
     */
    Mono<Void> stop();
}
