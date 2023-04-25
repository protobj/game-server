package io.protobj.services.discovery.scalecube.member;

import io.protobj.services.discovery.scalecube.event.ClusterMemberEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MemberService {
    Flux<ClusterMemberEvent> listen();

    Mono<Void> start();

    Mono<Void> shutdown();
}
