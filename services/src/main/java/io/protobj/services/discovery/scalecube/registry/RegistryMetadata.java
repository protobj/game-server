package io.protobj.services.discovery.scalecube.registry;

import reactor.util.function.Tuple2;

import java.util.HashMap;
import java.util.Map;

public class RegistryMetadata {


    private Map<Tuple2<Integer, Integer>, Integer> gid2st2tgtSid = new HashMap<>();

    private long timestamp;

    public Map<Tuple2<Integer, Integer>, Integer> getGid2st2tgtSid() {
        return gid2st2tgtSid;
    }

    public RegistryMetadata setGid2st2tgtSid(Map<Tuple2<Integer, Integer>, Integer> gid2st2tgtSid) {
        this.gid2st2tgtSid = gid2st2tgtSid;
        return this;
    }


    public long getTimestamp() {
        return timestamp;
    }

    public RegistryMetadata setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }
}
