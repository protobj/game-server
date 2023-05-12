package io.protobj.services.discovery.scalecube.metadata;

import io.protobj.services.ServiceEndPoint;

public class MemberMetadata {
    private int[] slots;
    private ServiceEndPoint serviceEndPoint;

    public int[] getSlots() {
        return slots;
    }

    public void setSlots(int[] slots) {
        this.slots = slots;
    }

    public ServiceEndPoint getServiceEndPoint() {
        return serviceEndPoint;
    }

    public MemberMetadata setServiceEndPoint(ServiceEndPoint serviceEndPoint) {
        this.serviceEndPoint = serviceEndPoint;
        return this;
    }
}
