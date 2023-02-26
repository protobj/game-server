package io.protobj.network.internal.message;

import java.util.List;

public record MulticastMessage(List<Integer> channelIds, Object msg) {
}
