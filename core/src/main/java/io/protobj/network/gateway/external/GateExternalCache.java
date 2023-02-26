package io.protobj.network.gateway.external;

import org.apache.commons.collections4.MapUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class GateExternalCache {

    private final AtomicInteger idGenerator = new AtomicInteger();

    private final Map<Integer, Map<Integer, GateExternalSession>> allChannels = new ConcurrentHashMap<>();

    public int generateId() {
        return idGenerator.getAndIncrement();
    }

    public void putSession(GateExternalSession gateExternalSession) {
        Map<Integer, GateExternalSession> sessionMap = allChannels.computeIfAbsent(gateExternalSession.getSid(), t -> new ConcurrentHashMap<>());
        sessionMap.put(gateExternalSession.getId(), gateExternalSession);
    }

    public GateExternalSession getSession(int frontId, int sid) {
        Map<Integer, GateExternalSession> frontSessionMap = allChannels.get(sid);
        if (MapUtils.isEmpty(frontSessionMap)) {
            return null;
        }
        return frontSessionMap.get(frontId);
    }

    public Map<Integer, GateExternalSession> getSessions(int sid) {
        return allChannels.get(sid);
    }

    public Map<Integer, GateExternalSession> removeSessions(int sid) {
        return allChannels.remove(sid);
    }

    public void remove(GateExternalSession gateExternalSession) {
        Map<Integer, GateExternalSession> sessionMap = allChannels.get(gateExternalSession.getSid());
        if (sessionMap != null) {
            sessionMap.remove(gateExternalSession.getId());
        }
    }
}
