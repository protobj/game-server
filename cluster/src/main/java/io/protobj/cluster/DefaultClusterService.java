package io.protobj.cluster;

import io.protobj.cluster.api.ClusterService;
import io.protobj.cluster.config.ClusterConfig;
import io.protobj.cluster.discovery.MemberService;
import io.protobj.cluster.discovery.ScalecubeMemberService;
import io.protobj.cluster.event.ClusterMemberEvent;
import io.protobj.cluster.metadata.MemberMetadata;
import io.protobj.hash.SortedArrayList;
import io.protobj.hash.VirtualNode;
import io.protobj.services.ServiceEndPoint;
import io.protobj.services.util.IpUtils;
import io.scalecube.net.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;

import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.StampedLock;

public class DefaultClusterService implements ClusterService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultClusterService.class);
    private MemberService memberService;

    private Scheduler scheduler;

    private Map<Integer, ClusterMemberEvent> memberEventMap;

    public void start() {
        ClusterConfig config = new ClusterConfig();
        ScalecubeMemberService discoveryService = new ScalecubeMemberService();
        discoveryService
                .options(clusterConfig -> clusterConfig.metadata(config.getMetadata()))
                .options(clusterConfig -> clusterConfig.externalHost(IpUtils.getHost(config.isUseExternHost())))
                .options(clusterConfig -> clusterConfig.membership(membershipConfig -> membershipConfig.seedMembers(config.getSeedMembers())))
                .options(clusterConfig -> clusterConfig.transport(transportConfig -> transportConfig.port(config.getPort())))
                .start()
                .subscribe()
        ;
        memberEventMap = new HashMap<>();
        scheduler = Schedulers.newSingle("cluster-member", true);
        this.memberService = discoveryService;
        this.memberService.listen()
                .onBackpressureBuffer()
                .subscribeOn(scheduler)
                .publishOn(scheduler)
                .subscribe(this::onEvent);
    }


    private void onEvent(ClusterMemberEvent clusterMemberEvent) {
        MemberMetadata metadata = clusterMemberEvent.metadata();
        if (clusterMemberEvent.isAdded()) {
            memberEventMap.get(metadata)
        }
    }

    @Override
    public Mono<Void> register(ServiceEndPoint endPoint) {
        return null;
    }

    @Override
    public <T> Flux<T> updateClusterState() {
        return null;
    }

    @Override
    public Mono<Address> queryByGid(int st, int gid) {
        return null;
    }

    @Override
    public Mono<Address> queryBySid(int st, int sid) {
        return null;
    }

    @Override
    public Mono<Address> queryBySt(int st) {
        return null;
    }


    @Override
    public <T> Mono<T> requestResponse(int st, long slotKey, ByteBuffer header, ByteBuffer body) {
        return null;
    }

    @Override
    public void requestOne(int st, long slotKey, ByteBuffer header, ByteBuffer body) {

    }

    @Override
    public <T> Flux<T> requestStream(int st, long slotKey, ByteBuffer header, ByteBuffer body) {
        return null;
    }

    @Override
    public <T> Flux<T> requestChannel(int st, long slotKey, Flux<Tuple2<ByteBuffer, ByteBuffer>> request) {
        return null;
    }
}
