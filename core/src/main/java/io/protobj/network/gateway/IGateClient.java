package io.protobj.network.gateway;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

import java.util.concurrent.CompletableFuture;

public interface IGateClient {

    AttributeKey<CompletableFuture<Channel>> CONNECT_FUTURE = AttributeKey.newInstance("CONNECT_FUTURE");

    CompletableFuture<Channel> startTcpFrontClient(String host, int port);

    CompletableFuture<Channel> startTcpBackendClient(String host, int port);
}

