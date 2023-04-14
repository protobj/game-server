package io.protobj.hash;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.locks.StampedLock;

public class SlotRing {
    private static final Logger logger = LoggerFactory.getLogger(SlotRing.class);
    private final String ringName;
    public static final int MAX_SLOT = 16384;

    private final StampedLock lock = new StampedLock();

    //hash槽
    private final VirtualNode[] slots = new VirtualNode[MAX_SLOT];
    //判断集群是否填充满
    private final BitSet slotFlag = new BitSet(MAX_SLOT);
    // 通过startSlot排序，方便找出下一个
    private final SortedArrayList<VirtualNode> virtualNodes = new SortedArrayList<>(Comparator.comparing(VirtualNode::getStartSlot));

    private final Map<Integer, int[]> sid2slotsMap = new HashMap<>();

    public SlotRing(String ringName) {
        this.ringName = ringName;
    }

    public boolean checkSlots(int sid, int[] slots) {
        for (int i = 0; i < slots.length; i += 2) {
            int start = slots[i];
            int end = slots[i + 1];
            if (start > end || start < 0 || end >= MAX_SLOT) {
                logger.error("slots配置：{}-{},{} {}", ringName, sid, start, end);
                return false;
            }
        }
        return true;
    }

    public boolean addOrUpd(int sid, int[] slots) {
        if (!checkSlots(sid, slots)) {
            return false;
        }
        int[] ints = sid2slotsMap.get(sid);
        if (ints != null && Arrays.equals(ints, slots)) {
            return false;
        }
        //排除自身所占，查询槽点是否已被占用
        return false;
    }


    public StampedLock getLock() {
        return lock;
    }
}
