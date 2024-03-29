package io.protobj.services.discovery.api;

import io.protobj.services.discovery.scalecube.event.ClusterMemberEvent;
import io.protobj.services.discovery.scalecube.registry.AddressRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ServiceDiscovery {

    Flux<ClusterMemberEvent> listen();

    /**
     * Starting this {@code ServiceDiscovery} instance.
     *
     * @return started {@code ServiceDiscovery} instance
     */
    Mono<Void> start();

    /**
     * Shutting down this {@code ServiceDiscovery} instance.
     *
     * @return async signal of the result
     */
    Mono<Void> shutdown();

    Mono<Integer> requestAddress(int gid, int st);
}
