package io.protobj.network.gateway.backend.client.session;

import io.netty.channel.Channel;

import java.util.concurrent.Executor;

public class DirectSession implements Session {
    private Channel channel;

    private Executor executor;


    @Override
    public void sendMsg(Object msg) {

    }

    @Override
    public Executor executor() {
        return executor;
    }
}
