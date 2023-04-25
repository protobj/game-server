package io.protobj.services.discovery.scalecube.config;

import io.protobj.services.discovery.scalecube.metadata.MemberMetadata;
import io.scalecube.net.Address;

import java.util.List;

public class ClusterConfig {

    /**
     * 集群
     */
    private List<Address> seedMembers;

    private int port;

    private MemberMetadata metadata;

    private boolean useExternHost;

    public List<Address> getSeedMembers() {
        return seedMembers;
    }

    public void setSeedMembers(List<Address> seedMembers) {
        this.seedMembers = seedMembers;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public MemberMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(MemberMetadata metadata) {
        this.metadata = metadata;
    }

    public boolean isUseExternHost() {
        return useExternHost;
    }

    public void setUseExternHost(boolean useExternHost) {
        this.useExternHost = useExternHost;
    }
}
