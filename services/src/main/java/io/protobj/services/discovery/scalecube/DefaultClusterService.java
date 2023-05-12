package io.protobj.services.discovery.scalecube;

import io.protobj.services.discovery.scalecube.config.ClusterConfig;
import io.protobj.services.discovery.scalecube.event.ClusterMemberEvent;
import io.protobj.services.discovery.scalecube.hash.SlotRing;
import io.protobj.services.discovery.scalecube.metadata.MemberMetadata;
import io.protobj.services.ServiceEndPoint;
import io.protobj.services.util.IpUtils;
import io.scalecube.net.Address;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
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

public class DefaultClusterService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultClusterService.class);

    private Scheduler scheduler;



    public void start() {
        ClusterConfig config = new ClusterConfig();
        ScalecubeServiceDiscovery discoveryService = new ScalecubeServiceDiscovery();
        discoveryService
                .options(clusterConfig -> clusterConfig.metadata(config.getMetadata()))
                .options(clusterConfig -> clusterConfig.externalHost(IpUtils.getHost(config.isUseExternHost())))
                .options(clusterConfig -> clusterConfig.membership(membershipConfig -> membershipConfig.seedMembers(config.getSeedMembers())))
                .options(clusterConfig -> clusterConfig.transport(transportConfig -> transportConfig.port(config.getPort())))
                .start()
                .subscribe()
        ;
        scheduler = Schedulers.newSingle("cluster-member", true);

        discoveryService.listen()
                .onBackpressureBuffer()
                .subscribeOn(scheduler)
                .publishOn(scheduler)
                .subscribe(this::onEvent);
    }

    private void onEvent(ClusterMemberEvent clusterMemberEvent) {
        String id = clusterMemberEvent.member().id();
        MemberMetadata metadata = clusterMemberEvent.metadata();
        if (clusterMemberEvent.isAdded() || clusterMemberEvent.isUpdated()) {
            slotRing.addOrUpd(id, metadata.getSlots());
        } else if (clusterMemberEvent.isRemoved() || clusterMemberEvent.isLeaving()) {
            slotRing.delete(id);
        }
    }

    public Mono<Void> register(ServiceEndPoint endPoint) {
        return null;
    }

    public <T> Flux<T> updateClusterState() {
        return null;
    }

    public Mono<Address> queryByGid(int st, int gid) {
        return null;
    }

    public Mono<Address> queryBySid(int st, int sid) {
        return null;
    }

    public Mono<Address> queryBySt(int st) {
        return null;
    }


    public <T> Mono<T> requestResponse(int st, long slotKey, ByteBuffer header, ByteBuffer body) {
        return null;
    }

    public void requestOne(int st, long slotKey, ByteBuffer header, ByteBuffer body) {

    }

    public <T> Flux<T> requestStream(int st, long slotKey, ByteBuffer header, ByteBuffer body) {
        return null;
    }

    public <T> Flux<T> requestChannel(int st, long slotKey, Flux<Tuple2<ByteBuffer, ByteBuffer>> request) {
        return null;
    }
}
