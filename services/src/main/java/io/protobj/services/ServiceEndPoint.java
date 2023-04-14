package io.protobj.services;

import io.scalecube.net.Address;

import java.util.BitSet;
import java.util.List;

public class ServiceEndPoint {
    private int st;//服务类型
    private int sid;//服务id
    private int gid;//服务组id
    private int loadRate;//负载
    private List<Integer> slots;//可选参数
    private volatile transient int fullId;
    private Address address;

    private transient BitSet slotBits;

    public int fullId() {
        if (fullId != 0) {
            return fullId;
        }
        return fullId = st * 10000 + sid;
    }

    public synchronized BitSet slotBits() {
        if (slotBits != null) {
            return slotBits;
        }
        slotBits = new BitSet();
        if (slots != null && slots.size() > 0) {
            for (int i = 0; i < slots.size(); i += 2) {
                slotBits.set(slots.get(i), slots.get(i + 1) + 1);
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

    public List<Integer> getSlots() {
        return slots;
    }

    public void setSlots(List<Integer> slots) {
        this.slots = slots;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

}
