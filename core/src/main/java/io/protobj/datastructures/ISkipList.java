package io.protobj.datastructures;

import java.util.List;
import java.util.function.Consumer;

public interface ISkipList<E> {
    int SKIPLIST_MAXLEVEL = 32;
    double SKIPLIST_P = 0.25;

    void insert(E ele);

    void delete(E ele);

    long getRank(E ele);

    List<E> limit(int count);

    void forEach(Consumer<E> consumer);

    default SkipListNode<E> createNode(int level, E ele) {
        SkipListNode<E> skipListNode = new SkipListNode<>();
        SkipListLevel[] level1 = new SkipListLevel[level];
        for (int i = 0; i < level1.length; i++) {
            level1[i] = new SkipListLevel();
        }
        skipListNode.level = level1;
        skipListNode.ele = ele;
        return skipListNode;
    }


    class SkipListNode<E> {
        SkipListNode<E> backward;
        E ele;
        SkipListLevel<E>[] level;
    }

    class SkipListLevel<E> {
        SkipListNode<E> forward;
        int span;
    }

    interface RankEle extends Comparable<RankEle> {
        String owner();
    }
}
