package io.protobj.network.gateway.front;

public enum FrontCommand {
    Null(0),
    Handshake(1),//握手
    Heartbeat(2),//心跳
    ERR(3),//错误返回
    Forward(4),//转发

    Close(5),//连接关闭
    ;

    private byte command;

    FrontCommand(int command) {
        this.command = (byte) command;
    }

    public byte getCommand() {
        return command;
    }
}
