package io.protobj.services;

import io.protobj.services.methods.ServiceMethodInvoker;
import io.scalecube.net.Address;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import java.util.BitSet;

public class ServiceEndPoint {
    private int st;//服务类型
    private int sid;//服务id
    private int gid;//服务组id
    private int loadRate;//负载
    private int[] slots;//可选参数
    private Address address;
    private transient BitSet slotBits;
    private transient Int2ObjectMap<ServiceMethodInvoker> invokerMap;

    public synchronized BitSet slotBits() {
        if (slotBits != null) {
            return slotBits;
        }
        slotBits = new BitSet();
        if (slots != null && slots.length > 0) {
            for (int i = 0; i < slots.length; i += 2) {
                slotBits.set(slots[i], slots[i + 1] + 1);
            }
        }
        return slotBits;
    }

    public int getSt() {
        return st;
    }

    public void setSt(int st) {
        this.st = st;
    }

    public int getSid() {
        return sid;
    }

    public void setSid(int sid) {
        this.sid = sid;
    }

    public int getGid() {
        return gid;
    }

    public void setGid(int gid) {
        this.gid = gid;
    }

    public int getLoadRate() {
        return loadRate;
    }

    public void setLoadRate(int loadRate) {
        this.loadRate = loadRate;
    }

    public int[] getSlots() {
        return slots;
    }

    public void setSlots(int[] slots) {
        this.slots = slots;
    }

    public BitSet getSlotBits() {
        return slotBits;
    }

    public void setSlotBits(BitSet slotBits) {
        this.slotBits = slotBits;
    }

    public Int2ObjectMap<ServiceMethodInvoker> getInvokerMap() {
        return invokerMap;
    }

    public void setInvokerMap(Int2ObjectMap<ServiceMethodInvoker> invokerMap) {
        this.invokerMap = invokerMap;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public ServiceMethodInvoker getInvoker(int cmd) {
        return invokerMap.get(cmd);
    }

    @Override
    public String toString() {
        return "ServiceEndPoint{" +
                "st=" + st +
                ", sid=" + sid +
                ", gid=" + gid +
                ", loadRate=" + loadRate +
                ", address=" + address +
                '}';
    }
}
