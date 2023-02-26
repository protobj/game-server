package io.protobj.network.internal;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

import java.util.concurrent.CompletableFuture;

public interface InternalClient {
    AttributeKey<Integer> SID = AttributeKey.newInstance("sid");
    AttributeKey<CompletableFuture<Channel>> CONNECT_FUTURE = AttributeKey.newInstance("CONNECT_FUTURE");

    CompletableFuture<Channel> start(String host, int port, int sid);
}

