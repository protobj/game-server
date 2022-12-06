package io.protobj.network.gateway.backend;

public enum BackendCommand {
    Null(0),
    Forward(1),//转发
    Heartbeat(2),//心跳
    Handshake(3),//握手
    Unicast(4),//单播
    Multicast(5),//组播
    Broadcast(6),//广播

    Close(7),//连接关闭
    ;

    private byte command;

    BackendCommand(int command) {
        this.command = (byte) command;
    }

    public byte getCommand() {
        return command;
    }
}
