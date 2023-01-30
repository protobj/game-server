package io.protobj.network.gateway.backend.client.session;

public interface SessionCache {

    Session getSessionById(int channelId);
}
