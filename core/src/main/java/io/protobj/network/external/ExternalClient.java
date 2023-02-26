package io.protobj.network.external;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

import java.util.concurrent.CompletableFuture;

public interface ExternalClient {
    AttributeKey<CompletableFuture<Channel>> CONNECT_FUTURE = AttributeKey.newInstance("CONNECT_FUTURE");

    CompletableFuture<Channel> connectTcp(String host, int port);

    CompletableFuture<Channel> connectKcp(String host, int port);

    CompletableFuture<Channel> connectWebsocket(String host, int port);
}
