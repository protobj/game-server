package io.protobj.services.discovery.scalecube.event;

import io.protobj.services.discovery.scalecube.metadata.MemberMetadata;
import io.scalecube.cluster.Member;

import java.util.Objects;

public class ClusterMemberEvent {
    public enum Type {
        ADDED,
        REMOVED,
        LEAVING,
        UPDATED
    }

    private final Type type;

    private final Member member;
    private final MemberMetadata metadata;

    public ClusterMemberEvent(Type type, io.scalecube.cluster.Member member, MemberMetadata metadata) {
        this.type = Objects.requireNonNull(type, "ServiceDiscoveryEvent: type");
        this.member = Objects.requireNonNull(member, "member must be not null");
        this.metadata =
                Objects.requireNonNull(metadata, "ServiceDiscoveryEvent: serviceEndpoint");
    }

    public static ClusterMemberEvent newAdded(io.scalecube.cluster.Member member, MemberMetadata metadata) {
        Objects.requireNonNull(member, "member must be not null");
        return new ClusterMemberEvent(Type.ADDED, member, metadata);
    }

    public static ClusterMemberEvent newRemoved(io.scalecube.cluster.Member member, MemberMetadata metadata) {
        Objects.requireNonNull(member, "member must be not null");
        return new ClusterMemberEvent(Type.REMOVED, member, metadata);
    }

    public static ClusterMemberEvent newLeaving(io.scalecube.cluster.Member member, MemberMetadata metadata) {
        Objects.requireNonNull(member, "member must be not null");
        return new ClusterMemberEvent(Type.LEAVING, member, metadata);
    }

    public static ClusterMemberEvent newUpdated(io.scalecube.cluster.Member member, MemberMetadata metadata) {
        Objects.requireNonNull(member, "member must be not null");
        return new ClusterMemberEvent(Type.UPDATED, member, metadata);
    }

    public Type type() {
        return type;
    }

    public boolean isAdded() {
        return type == Type.ADDED;
    }

    public boolean isRemoved() {
        return type == Type.REMOVED;
    }

    public boolean isLeaving() {
        return type == Type.LEAVING;
    }

    public boolean isUpdated() {
        return type == Type.UPDATED;
    }

    public io.scalecube.cluster.Member member() {
        return member;
    }

    public MemberMetadata metadata() {
        return metadata;
    }

    @Override
    public String toString() {
        return "ClusterEvent{" +
                "type=" + type +
                ", member=" + member +
                ", metadata=" + metadata +
                '}';
    }
}
