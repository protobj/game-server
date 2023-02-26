package io.protobj;

import io.protobj.network.internal.session.Session;
import io.protobj.network.internal.session.SessionCache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager implements SessionCache {

    private final Map<Integer, Session> sessionMap = new ConcurrentHashMap<>();

    @Override
    public Session getSessionById(int channelId) {
        return sessionMap.get(channelId);
    }
}
