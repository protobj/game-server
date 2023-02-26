package io.protobj.network.internal.session;

import io.netty.channel.Channel;

public class DirectSession extends Session {
    private Channel channel;
    @Override
    public Channel getChannel() {
        return channel;
    }

}
