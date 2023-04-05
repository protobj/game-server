package io.protobj.cluster.discovery;

import io.scalecube.cluster.membership.MembershipEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ServiceDiscovery {
    Flux<MembershipEvent> listen();

    Mono<Void> start();

    Mono<Void> shutdown();
}
