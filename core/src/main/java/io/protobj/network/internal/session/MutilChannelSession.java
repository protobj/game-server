package io.protobj.network.internal.session;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class MutilChannelSession {

    public static final AttributeKey<MutilChannelSession> MUTIL_CHANNEL_KEY = AttributeKey.newInstance("MUTIL_CHANNEL_KEY");

    private List<Channel> channels = new CopyOnWriteArrayList<>();

    private AtomicInteger chooseIndex = new AtomicInteger();

    public void add(Channel channel) {
        this.channels.add(channel);
    }

    public void rm(Channel channel) {
        this.channels.remove(channel);
    }

    public Channel choose() {
        return channels.get(chooseIndex.getAndIncrement());
    }
}
