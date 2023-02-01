package io.protobj.msgdispatcher;

import io.protobj.msg.Message;

import java.util.concurrent.CompletableFuture;

public interface INetHandler {

    CompletableFuture<?> invoke(Message message) throws Throwable;
}
