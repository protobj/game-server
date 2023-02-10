package io.protobj.msgdispatcher;

import io.protobj.msg.Message;
import io.protobj.network.gateway.backend.client.session.Session;

import java.util.concurrent.CompletableFuture;

public interface INetHandler {

    CompletableFuture<?> invoke(Session session,Message message) throws Throwable;
}
