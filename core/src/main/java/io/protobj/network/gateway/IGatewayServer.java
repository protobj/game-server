package io.protobj.network.gateway;

import io.netty.util.AttributeKey;
import io.protobj.network.gateway.internal.GateInternalCache;
import io.protobj.network.gateway.internal.GateInternalSession;
import io.protobj.network.gateway.external.GateExternalCache;
import io.protobj.network.gateway.external.GateExternalSession;

import java.util.concurrent.CompletableFuture;

public interface IGatewayServer {
    AttributeKey<byte[]> TOKEN = AttributeKey.newInstance("TOKEN");
    AttributeKey<GateExternalSession> FRONT_SESSION = AttributeKey.newInstance("FRONT_SESSION");

    AttributeKey<GateInternalSession> BACKEND_SESSION = AttributeKey.newInstance("BACKEND_SESSION");

    /**
     * 开启前端tcp监听
     */
    CompletableFuture<Void> startTcpFrontServer(String host, int... ports);

    /**
     * 开启前端kcp监听
     */
    CompletableFuture<Void> startKcpFrontServer(String host, int... ports);

    CompletableFuture<Void> startWebsocketFrontServer(String host, int... ports);

    /**
     * 开启后端tcp监听
     */
    CompletableFuture<Void> startTcpBackendServer(String host, int... ports);

    GateInternalCache getBackendCache();

    GateExternalCache getFrontCache();
}
