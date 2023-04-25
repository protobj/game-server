package io.protobj.services.discovery.scalecube;

import io.protobj.services.discovery.scalecube.api.ClusterService;
import io.protobj.services.discovery.scalecube.config.ClusterConfig;
import io.protobj.services.discovery.scalecube.member.MemberService;
import io.protobj.services.discovery.scalecube.member.ScalecubeMemberService;
import io.protobj.services.discovery.scalecube.event.ClusterMemberEvent;
import io.protobj.services.discovery.scalecube.metadata.MemberMetadata;
import io.protobj.hash.SlotRing;
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
import java.util.HashMap;
import java.util.Map;

public class DefaultClusterService implements ClusterService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultClusterService.class);
    private MemberService memberService;

    private Scheduler scheduler;

    private Map<Integer, ClusterMemberEvent> memberEventMap;

    private SlotRing slotRing;

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
        slotRing = new SlotRing("cluster-member");
        this.memberService = discoveryService;
        this.memberService.listen()
                .onBackpressureBuffer()
                .subscribeOn(scheduler)
                .publishOn(scheduler)
                .subscribe(this::onEvent);
    }

    private void onEvent(ClusterMemberEvent clusterMemberEvent) {
        MemberMetadata metadata = clusterMemberEvent.metadata();
        if (clusterMemberEvent.isAdded() || clusterMemberEvent.isUpdated()) {
            memberEventMap.put(metadata.getId(), clusterMemberEvent);
            slotRing.addOrUpd(metadata.getId(), metadata.getSlots());
        } else if (clusterMemberEvent.isRemoved() || clusterMemberEvent.isLeaving()) {
            ClusterMemberEvent removed = memberEventMap.remove(metadata.getId());
            if (removed != null) {
                slotRing.delete(metadata.getId());
            }
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
