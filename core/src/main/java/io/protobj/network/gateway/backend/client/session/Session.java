package io.protobj.network.gateway.backend.client.session;

import java.util.concurrent.Executor;

public interface Session {

    void sendMsg(Object msg);

    Executor executor();
}
