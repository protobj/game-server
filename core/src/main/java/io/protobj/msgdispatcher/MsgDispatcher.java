package io.protobj.msgdispatcher;

import io.protobj.IServer;
import io.protobj.Module;
import io.protobj.network.internal.message.RqstMessage;
import io.protobj.network.internal.session.Session;

import java.util.List;

public interface MsgDispatcher {
    void init(List<Module> moduleList, IServer server);

    void dispatch(Session session, RqstMessage rqstMessage);
}
