package io.protobj.network.internal.message;

public record PushMessage(int channelId, Object msg) {
}
