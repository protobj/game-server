package io.protobj.cluster;

import java.util.List;

public class ClusterConfig {

    /**
     * 集群
     */
    private List<String> seedMembers;

    private String host;

    private int port;


    private ServiceMetaData metaData;

}
