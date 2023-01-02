package io.protobj.network.gateway;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.protobj.network.Command;

public class ErrorCode {

    public static byte UNKNOWN = 1;
    public static byte NOT_AUTH = 2;
    public static byte ERR_TOKEN = 3;
    public static byte SERVER_NOT_ONLINE = 4;

    public static byte ERR_COMMAND = 5;

    public static ByteBuf createErrorMsg(Channel channel, byte code) {
        ByteBuf buffer = channel.alloc().buffer(4);
        buffer.writeShort(2);
        buffer.writeByte(Command.ERR.getCommand());
        buffer.writeByte(code);
        return buffer;
    }
}
