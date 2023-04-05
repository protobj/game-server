package io.protobj.cluster;

import io.scalecube.cluster.Member;

public class ClusterMember {

    private ServiceInfo selfInfo;

    public ServiceInfo getSelfInfo() {
        return selfInfo;
    }

    public void setSelfInfo(ServiceInfo selfInfo) {
        this.selfInfo = selfInfo;
    }


}
