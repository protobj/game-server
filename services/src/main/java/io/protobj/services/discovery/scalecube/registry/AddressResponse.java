package io.protobj.services.discovery.scalecube.registry;

public class AddressResponse {

    private int sid;


    public AddressResponse(int sid) {
        this.sid = sid;
    }

    public AddressResponse() {
    }

    public int getSid() {
        return sid;
    }

    public AddressResponse setSid(int sid) {
        this.sid = sid;
        return this;
    }
}
