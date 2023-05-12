package io.protobj.services.discovery.scalecube;

import io.protobj.services.discovery.api.ServiceDiscovery;
import io.protobj.services.discovery.scalecube.event.ClusterMemberEvent;
import io.protobj.services.discovery.scalecube.hash.SlotRing;
import io.protobj.services.discovery.scalecube.metadata.MemberMetadata;
import io.protobj.services.discovery.scalecube.registry.AddressRequest;
import io.protobj.services.discovery.scalecube.registry.AddressResponse;
import io.protobj.services.discovery.scalecube.registry.RegistryMetadata;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import io.scalecube.cluster.Cluster;
import io.scalecube.cluster.ClusterConfig;
import io.scalecube.cluster.ClusterImpl;
import io.scalecube.cluster.ClusterMessageHandler;
import io.scalecube.cluster.fdetector.FailureDetectorConfig;
import io.scalecube.cluster.gossip.GossipConfig;
import io.scalecube.cluster.membership.MembershipConfig;
import io.scalecube.cluster.membership.MembershipEvent;
import io.scalecube.cluster.transport.api.Message;
import io.scalecube.cluster.transport.api.TransportConfig;
import io.scalecube.net.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.Map;
import java.util.function.UnaryOperator;

import static io.scalecube.reactor.RetryNonSerializedEmitFailureHandler.RETRY_NON_SERIALIZED;

public class ScalecubeServiceDiscovery implements ServiceDiscovery, ClusterMessageHandler {
    private static final Logger logger = LoggerFactory.getLogger(ScalecubeServiceDiscovery.class);

    private ClusterConfig clusterConfig;

    private Cluster cluster;

    private SlotRing<Address> slotRing;

    private RegistryMetadata registryMetadata;
    // Sink
    private final Sinks.Many<ClusterMemberEvent> sink =
            Sinks.many().multicast().directBestEffort();

    private final Schema<MemberMetadata> schema = RuntimeSchema.getSchema(MemberMetadata.class);

    public ScalecubeServiceDiscovery() {
        this.clusterConfig = ClusterConfig.defaultLanConfig();
        cluster.metadata()
    }

    private ScalecubeServiceDiscovery(ScalecubeServiceDiscovery other) {
        this.clusterConfig = other.clusterConfig;
        this.cluster = other.cluster;
    }

    public ScalecubeServiceDiscovery options(UnaryOperator<ClusterConfig> opts) {
        ScalecubeServiceDiscovery d = new ScalecubeServiceDiscovery(this);
        d.clusterConfig = opts.apply(clusterConfig);
        return d;
    }

    public ScalecubeServiceDiscovery transport(UnaryOperator<TransportConfig> opts) {
        return options(cfg -> cfg.transport(opts));
    }

    public ScalecubeServiceDiscovery membership(UnaryOperator<MembershipConfig> opts) {
        return options(cfg -> cfg.membership(opts));
    }

    public ScalecubeServiceDiscovery gossip(UnaryOperator<GossipConfig> opts) {
        return options(cfg -> cfg.gossip(opts));
    }


    public ScalecubeServiceDiscovery failureDetector(UnaryOperator<FailureDetectorConfig> opts) {
        return options(cfg -> cfg.failureDetector(opts));
    }

    @Override
    public Flux<ClusterMemberEvent> listen() {
        return sink.asFlux().onBackpressureBuffer();
    }

    @Override
    public Mono<Void> start() {
        slotRing = new SlotRing<>("cluster-member");
        return new ClusterImpl()
                .config(options -> clusterConfig)
                .handler(cluster -> this)
                .start()
                .doOnSuccess(cluster -> {
                    this.cluster = cluster;
                    logger.debug("cluster start success");
                })
                .then();
    }

    @Override
    public Mono<Void> shutdown() {
        return Mono.defer(
                () -> {
                    if (cluster == null) {
                        sink.emitComplete(RETRY_NON_SERIALIZED);
                        return Mono.empty();
                    }
                    cluster.shutdown();
                    return cluster.onShutdown().doFinally(s -> sink.emitComplete(RETRY_NON_SERIALIZED));
                });
    }

    @Override
    public Mono<Integer> requestAddress(int gid, int st) {
        return Mono.defer(() -> {
            Map<Tuple2<Integer, Integer>, Integer> gid2st2tgtSid =
                    registryMetadata.getGid2st2tgtSid();
            Integer sid = gid2st2tgtSid.get(Tuples.of(gid, st));
            if (sid != null) {
                return Mono.just(sid);
            }

            Address address = slotRing.hit(gid);
            if (address.equals(cluster.address())) {
                return findAddress(gid, st);
            }
            AddressRequest request = new AddressRequest(gid, st);
            Message message = Message.withData(request)
                    .correlationId(String.valueOf(System.currentTimeMillis()))
                    .build();
            return cluster.requestResponse(address, message).map(Message::data).cast(AddressResponse.class).map(AddressResponse::getSid);
        });

    }

    private Mono<Integer> findAddress(int gid, int st) {
        return null;
    }

    @Override
    public void onMessage(Message message) {
        ClusterMessageHandler.super.onMessage(message);
        if (message.data() instanceof AddressRequest) {
            AddressRequest request = message.data();
            requestAddress(request.getGid(), request.getSt())
            ;
        }
    }

    @Override
    public void onGossip(Message gossip) {
        ClusterMessageHandler.super.onGossip(gossip);
    }

    @Override
    public void onMembershipEvent(MembershipEvent event) {
        logger.debug("onMembershipEvent: {}", event);
        ClusterMemberEvent clusterMemberEvent = toClusterMemberEvent(event);
        if (clusterMemberEvent == null) {
            logger.warn(
                    "clusterMemberEvent is null, cannot publish it (corresponding MembershipEvent: {})",
                    event);
            return;
        }
        logger.debug("Publish clusterMemberEvent: {}", clusterMemberEvent);

        sink.emitNext(clusterMemberEvent, RETRY_NON_SERIALIZED);
    }

    private ClusterMemberEvent toClusterMemberEvent(MembershipEvent event) {
        ClusterMemberEvent clusterMemberEvent = null;
        if (event.isAdded() && event.newMetadata() != null) {
            MemberMetadata metadata = new MemberMetadata();
            ProtostuffIOUtil.mergeFrom(event.newMetadata().array(), metadata, schema);
            clusterMemberEvent = ClusterMemberEvent.newAdded(event.member(), metadata);
        } else if (event.isRemoved() && event.oldMetadata() != null) {
            MemberMetadata metadata = new MemberMetadata();
            ProtostuffIOUtil.mergeFrom(event.oldMetadata().array(), metadata, schema);
            return ClusterMemberEvent.newRemoved(event.member(), metadata);
        } else if (event.isLeaving() && event.newMetadata() != null) {
            MemberMetadata metadata = new MemberMetadata();
            ProtostuffIOUtil.mergeFrom(event.newMetadata().array(), metadata, schema);
            return ClusterMemberEvent.newLeaving(event.member(), metadata);
        } else if (event.isUpdated() && event.newMetadata() != null) {
            MemberMetadata metadata = new MemberMetadata();
            ProtostuffIOUtil.mergeFrom(event.newMetadata().array(), metadata, schema);
            return ClusterMemberEvent.newAdded(event.member(), metadata);
        }
        if (clusterMemberEvent != null) {
            MemberMetadata metadata = clusterMemberEvent.metadata();
            if (clusterMemberEvent.isAdded() || clusterMemberEvent.isUpdated()) {
                slotRing.addOrUpd(cluster.member().address(), metadata.getSlots());
            } else if (clusterMemberEvent.isRemoved() || clusterMemberEvent.isLeaving()) {
                slotRing.delete(cluster.member().address());
            }
        }
        return clusterMemberEvent;
    }
}
