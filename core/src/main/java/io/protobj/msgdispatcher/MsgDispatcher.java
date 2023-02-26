package io.protobj.msgdispatcher;

import io.protobj.network.internal.message.RqstMessage;
import io.protobj.network.internal.session.Session;

public interface MsgDispatcher {

    void dispatch(Session session, RqstMessage rqstMessage);
}
