package io.protobj.services.discovery.scalecube.registry;

public class AddressRequest {
    private int gid;

    private int st;


    public AddressRequest(int gid, int st) {
        this.gid = gid;
        this.st = st;
    }


    public AddressRequest() {
    }

    public int getGid() {
        return gid;
    }

    public AddressRequest setGid(int gid) {
        this.gid = gid;
        return this;
    }

    public int getSt() {
        return st;
    }

    public AddressRequest setSt(int st) {
        this.st = st;
        return this;
    }
}
