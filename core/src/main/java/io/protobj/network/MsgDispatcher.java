package io.protobj.network;

import io.protobj.network.gateway.backend.client.session.Session;

public interface MsgDispatcher {

    void dispatch(Session session, Object msg);
}
