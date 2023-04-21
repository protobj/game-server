package io.protobj.cluster.member;

import io.protobj.cluster.event.ClusterMemberEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MemberService {
    Flux<ClusterMemberEvent> listen();

    Mono<Void> start();

    Mono<Void> shutdown();
}
