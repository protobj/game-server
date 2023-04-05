package io.protobj.cluster.message;

public class Message {

    private Object data;

    private int ix;

    private long slotKey;

    private transient int fullSid;

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public int getIx() {
        return ix;
    }

    public void setIx(int ix) {
        this.ix = ix;
    }

    public int getFullSid() {
        return fullSid;
    }

    public void setFullSid(int fullSid) {
        this.fullSid = fullSid;
    }

    public long getSlotKey() {
        return slotKey;
    }

    public void setSlotKey(long slotKey) {
        this.slotKey = slotKey;
    }
}
