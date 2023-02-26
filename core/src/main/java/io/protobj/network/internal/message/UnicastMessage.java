package io.protobj.network.internal.message;

public record UnicastMessage(int channelId, int index, Object msg) {
}
