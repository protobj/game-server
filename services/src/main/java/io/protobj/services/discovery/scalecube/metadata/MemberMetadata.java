package io.protobj.services.discovery.scalecube.metadata;

import io.scalecube.net.Address;

public class MemberMetadata {
    private int id;
    private Address address;
    private int[] slots;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int[] getSlots() {
        return slots;
    }

    public void setSlots(int[] slots) {
        this.slots = slots;
    }

}
