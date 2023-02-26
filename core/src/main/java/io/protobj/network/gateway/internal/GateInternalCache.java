package io.protobj.network.gateway.internal;

import io.netty.channel.Channel;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class GateInternalCache {

    private final Map<Integer, List<GateInternalSession>> channels = new ConcurrentHashMap<>();


    public List<GateInternalSession> getServerSession(int sid) {
        return channels.get(sid);
    }

    public GateInternalSession putSession(int sid, Channel channel) {
        List<GateInternalSession> channelList = channels.computeIfAbsent(sid, t -> new CopyOnWriteArrayList<>());
        GateInternalSession session = new GateInternalSession(sid, channel);
        channelList.add(session);
        return session;
    }
}
