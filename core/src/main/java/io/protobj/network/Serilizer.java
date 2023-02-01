package io.protobj.network;

import io.netty.buffer.ByteBuf;
import io.protobj.msg.Message;

public interface Serilizer {


    ByteBuf toByteArray(Message msg);

    Message toObject(ByteBuf buf);
}
