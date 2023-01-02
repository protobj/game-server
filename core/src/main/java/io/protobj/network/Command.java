package io.protobj.network;

public enum Command {
    Null(0),
    Forward(1),//转发
    Heartbeat(2),//心跳
    Handshake(3),//握手
    Unicast(4),//单播
    Multicast(5),//组播
    Broadcast(6),//广播
    Close(7),//连接关闭
    ERR(8),//错误返回
    ;

    private final byte command;

    Command(int command) {
        this.command = (byte) command;
    }

    public static Command valueOf(byte cmd) {
        Command[] values = values();
        if (cmd <= 0 || cmd >= values.length) {
            return null;
        }
        return values[cmd];
    }

    public byte getCommand() {
        return command;
    }
}
