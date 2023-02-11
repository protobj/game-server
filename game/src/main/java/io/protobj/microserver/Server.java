package io.protobj.microserver;

import io.protobj.AServer;

public class Server extends AServer {

    private final ServerType serverType;

    private final int sid;

    public Server(ServerType serverType, int sid) {
        this.serverType = serverType;
        this.sid = sid;
    }

    @Override
    protected void preStart() {

    }

    @Override
    protected void postStart() {

    }

    @Override
    protected void initNet() {

    }

    public ServerType getServerType() {
        return serverType;
    }


    public int getSid() {
        return sid;
    }

}
