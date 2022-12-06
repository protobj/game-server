package io.protobj.network.gateway.front.server;

import org.apache.commons.collections4.MapUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class FrontServerCache {

    private final AtomicInteger idGenerator = new AtomicInteger();

    private final Map<Integer, Map<Integer, FrontServerSession>> allChannels = new ConcurrentHashMap<>();

    public int generateId() {
        return idGenerator.getAndIncrement();
    }

    public void putSession(FrontServerSession frontServerSession) {
        Map<Integer, FrontServerSession> sessionMap = allChannels.computeIfAbsent(frontServerSession.getSid(), t -> new ConcurrentHashMap<>());
        sessionMap.put(frontServerSession.getId(), frontServerSession);
    }

    public FrontServerSession getSession(int frontId, int sid) {
        Map<Integer, FrontServerSession> frontSessionMap = allChannels.get(sid);
        if (MapUtils.isEmpty(frontSessionMap)) {
            return null;
        }
        return frontSessionMap.get(frontId);
    }

    public Map<Integer, FrontServerSession> getSessions(int sid) {
        return allChannels.get(sid);
    }

    public Map<Integer, FrontServerSession> removeSessions(int sid) {
        return allChannels.remove(sid);
    }

    public void remove(FrontServerSession frontServerSession) {
        Map<Integer, FrontServerSession> sessionMap = allChannels.get(frontServerSession.getSid());
        if (sessionMap != null) {
            sessionMap.remove(frontServerSession.getId());
        }
    }
}
