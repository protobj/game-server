package io.protobj.microserver;

public class ServerConf {
    private ServerType ServerType;

    private int svrId;

    private String slots;


    public ServerType getServerType() {
        return ServerType;
    }

    public void setServerType(ServerType ServerType) {
        this.ServerType = ServerType;
    }

    public int getSvrId() {
        return svrId;
    }

    public void setSvrId(int svrId) {
        this.svrId = svrId;
    }

    public String getSlots() {
        return slots;
    }

    public void setSlots(String slots) {
        this.slots = slots;
    }
}
