package io.protobj.microserver;

public class ServerConf {
    private ServerType svrType;

    private int svrId;

    private String slots;


    public ServerType getSvrType() {
        return svrType;
    }

    public void setSvrType(ServerType svrType) {
        this.svrType = svrType;
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
