package io.protobj.hash;

public class VirtualNode {

    private final int sid;
    private int startSlot;
    private final int endSlot;

    public VirtualNode(int startSlot, int endSlot, int sid) {
        super();
        this.sid = sid;
        this.startSlot = startSlot;
        this.endSlot = endSlot;
    }

    public void setStartSlot(int startSlot) {
        this.startSlot = startSlot;
    }

    public int getStartSlot() {
        return startSlot;
    }

    public int getEndSlot() {
        return endSlot;
    }

    public int size() {
        return endSlot - startSlot + 1;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + endSlot;
        result = prime * result + startSlot;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        VirtualNode other = (VirtualNode) obj;
        if (endSlot != other.endSlot)
            return false;
        if (startSlot != other.startSlot)
            return false;
        return true;
    }

    public int getSid() {
        return sid;
    }

    @Override
    public String toString() {
        return "[" + startSlot + "-" + endSlot + "]";
    }


}
