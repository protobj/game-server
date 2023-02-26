package io.protobj.network;

import io.netty.buffer.ByteBuf;

public interface Serializer {

    ByteBuf toByteArray(Object msg);

    Object toObject(ByteBuf buf);
}
