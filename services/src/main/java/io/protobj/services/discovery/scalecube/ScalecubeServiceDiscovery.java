package io.protobj.services.discovery.scalecube;

import io.protobj.services.discovery.api.ServiceDiscovery;
import io.protobj.services.discovery.scalecube.event.ClusterMemberEvent;
import io.protobj.services.discovery.scalecube.metadata.MemberMetadata;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.function.UnaryOperator;

import static io.scalecube.reactor.RetryNonSerializedEmitFailureHandler.RETRY_NON_SERIALIZED;

public class ScalecubeServiceDiscovery implements ServiceDiscovery, ClusterMessageHandler {
    private static final Logger logger = LoggerFactory.getLogger(ScalecubeServiceDiscovery.class);

    private ClusterConfig clusterConfig;

    private Cluster cluster;

    // Sink
    private final Sinks.Many<ClusterMemberEvent> sink =
            Sinks.many().multicast().directBestEffort();

    private final Schema<MemberMetadata> schema = RuntimeSchema.getSchema(MemberMetadata.class);

    public ScalecubeServiceDiscovery() {
        this.clusterConfig = ClusterConfig.defaultLanConfig();
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
    public void onMessage(Message message) {
        ClusterMessageHandler.super.onMessage(message);
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

        if (event.isAdded() && event.newMetadata() != null) {
            MemberMetadata metadata = new MemberMetadata();
            ProtostuffIOUtil.mergeFrom(event.newMetadata().array(), metadata, schema);
            return ClusterMemberEvent.newAdded(event.member(), metadata);
        }
        if (event.isRemoved() && event.oldMetadata() != null) {
            MemberMetadata metadata = new MemberMetadata();
            ProtostuffIOUtil.mergeFrom(event.oldMetadata().array(), metadata, schema);
            return ClusterMemberEvent.newRemoved(event.member(), metadata);
        }
        if (event.isLeaving() && event.newMetadata() != null) {
            MemberMetadata metadata = new MemberMetadata();
            ProtostuffIOUtil.mergeFrom(event.newMetadata().array(), metadata, schema);
            return ClusterMemberEvent.newLeaving(event.member(), metadata);
        }

        if (event.isUpdated() && event.newMetadata() != null) {
            MemberMetadata metadata = new MemberMetadata();
            ProtostuffIOUtil.mergeFrom(event.newMetadata().array(), metadata, schema);
            return ClusterMemberEvent.newAdded(event.member(), metadata);
        }

        return null;
    }
}
