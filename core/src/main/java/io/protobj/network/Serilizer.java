package io.protobj.network;

import io.netty.buffer.ByteBuf;

public interface Serilizer {

    ByteBuf toByteArray(Object msg);

    Object toObject(ByteBuf buf);
}
