package io.protobj.network.gateway;

import io.netty.util.AttributeKey;
import io.protobj.network.gateway.backend.server.BackendServerCache;
import io.protobj.network.gateway.backend.server.BackendServerSession;
import io.protobj.network.gateway.front.server.FrontServerCache;
import io.protobj.network.gateway.front.server.FrontServerSession;

import java.util.concurrent.CompletableFuture;

public interface IGatewayServer {
    AttributeKey<byte[]> TOKEN = AttributeKey.newInstance("TOKEN");
    AttributeKey<FrontServerSession> FRONT_SESSION = AttributeKey.newInstance("FRONT_SESSION");

    AttributeKey<BackendServerSession> BACKEND_SESSION = AttributeKey.newInstance("BACKEND_SESSION");

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

    BackendServerCache getBackendCache();

    FrontServerCache getFrontCache();
}
