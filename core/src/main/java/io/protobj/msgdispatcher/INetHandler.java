package io.protobj.msgdispatcher;

import io.protobj.network.internal.message.RqstMessage;
import io.protobj.network.internal.session.Session;

import java.util.concurrent.CompletableFuture;

public interface INetHandler {

    CompletableFuture<?> invoke(Session session, RqstMessage rqstMessage) throws Throwable;
}
