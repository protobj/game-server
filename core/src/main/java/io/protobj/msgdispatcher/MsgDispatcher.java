package io.protobj.msgdispatcher;

import io.protobj.msg.Message;
import io.protobj.network.gateway.backend.client.session.Session;

public interface MsgDispatcher {

    void dispatch(Session session, Message message);
}
