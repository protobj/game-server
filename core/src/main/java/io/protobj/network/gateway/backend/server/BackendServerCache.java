package io.protobj.network.gateway.backend.server;

import io.netty.channel.Channel;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class BackendServerCache {

    private final Map<Integer, List<BackendServerSession>> channels = new ConcurrentHashMap<>();


    public List<BackendServerSession> getServerSession(int sid) {
        return channels.get(sid);
    }

    public BackendServerSession putSession(int sid, Channel channel) {
        List<BackendServerSession> channelList = channels.computeIfAbsent(sid, t -> new CopyOnWriteArrayList<>());
        BackendServerSession session = new BackendServerSession(sid, channel);
        channelList.add(session);
        return session;
    }
}
