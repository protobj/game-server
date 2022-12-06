package io.protobj.datastructures;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class SkipList<E extends ISkipList.RankEle> implements ISkipList<E> {

    private int level = 1;
    private int length = 0;
    private SkipListNode<E> header = createNode(SKIPLIST_MAXLEVEL, null);
    private SkipListNode<E> tail;
    private Comparator<E> comparator;

    public SkipList(Comparator<E> comparator) {
        this.comparator = comparator;
    }

    public SkipList() {
    }

    public void clear() {
        this.level = 1;
        this.header = createNode(SKIPLIST_MAXLEVEL, null);
        this.tail = null;
        this.length = 0;
    }

    int randomLevel() {
        int level = 1;
        while ((random() & 0xFFFF) < SKIPLIST_P * 0xFFFF)
            level += 1;
        if (level < SKIPLIST_MAXLEVEL)
            return level;
        else
            return SKIPLIST_MAXLEVEL;
    }

    private int random() {
        return ThreadLocalRandom.current().nextInt();
    }

    @Override
    public void insert(E ele) {
        SkipListNode<E>[] update = new SkipListNode[SKIPLIST_MAXLEVEL];
        int[] rank = new int[SKIPLIST_MAXLEVEL];
        SkipListNode<E> x = this.header;
        for (int i = this.level - 1; i >= 0; i--) {
            if (this.level - 1 == i) {
                rank[i] = 0;
            } else {
                rank[i] = rank[i + 1];
            }
            SkipListNode<E> tempNode = null;
            if (x != null) {
                tempNode = x.level[i].forward;
            }
            while (tempNode != null) {
                int cmp = cmp(tempNode.ele, ele);
                if (cmp == 0) {
                    throw new RuntimeException("insert same element ${tempNode.element} $element");
                }
                if (cmp < 0) {
                    rank[i] += x.level[i].span;
                    x = tempNode;
                    tempNode = x.level[i].forward;
                } else {
                    break;
                }
            }
            update[i] = x;
        }
        int level = randomLevel();
        if (level > this.level) {
            for (int i = this.level; i < level; i++) {
                rank[i] = 0;
                update[i] = this.header;
                if (update[i] != null) {
                    update[i].level[i].span = this.length;
                }
            }
            this.level = level;
        }
        SkipListNode<E> newNode = createNode(level, ele);
        for (int i = 0; i < level; i++) {
            if (update[i] != null) {
                newNode.level[i].forward = update[i].level[i].forward;
                update[i].level[i].forward = newNode;
                newNode.level[i].span = update[i].level[i].span - (rank[0] - rank[i]);
                update[i].level[i].span = (rank[0] - rank[i]) + 1;
            }
        }
        for (int i = level; i < this.level; i++) {
            if (update[i] != null) {
                update[i].level[i].span++;
            }
        }
        if (update[0] != this.header) {
            newNode.backward = update[0];
        }
        if (newNode.level[0].forward != null) {
            newNode.level[0].forward.backward = newNode;
        } else {
            this.tail = newNode;
        }
        this.length++;
    }

    private int cmp(E ele1, E ele2) {
        if (ele1 == null) {
            throw new RuntimeException("元素不该为空");
        }
        if (comparator != null) {
            return comparator.compare(ele1, ele2);
        }
        return ele1.compareTo(ele2);
    }

    @Override
    public void delete(E ele) {
        SkipListNode<E>[] update = new SkipListNode[SKIPLIST_MAXLEVEL];
        SkipListNode<E> x = this.header;
        for (int i = this.level - 1; i >= 0; i--) {
            SkipListNode<E> forward = null;
            if (x != null) {
                forward = x.level[i].forward;
            }
            while (forward != null && cmp(forward.ele, ele) < 0) {
                x = forward;
                forward = x.level[i].forward;
            }
            update[i] = x;
        }
        if (x != null) {
            x = x.level[0].forward;
        }
        if (x != null) {
            if (cmp(x.ele, ele) == 0) {
                for (int i = 0; i < this.level; i++) {
                    if (update[i] != null) {
                        if (update[i].level[i].forward == x) {
                            update[i].level[i].span += x.level[i].span - 1;
                            update[i].level[i].forward = x.level[i].forward;
                        } else {
                            update[i].level[i].span -= 1;
                        }
                    }
                }
                if (x.level[0].forward != null) {
                    x.level[0].forward.backward = x.backward;
                } else {
                    this.tail = x.backward;
                }
                while (this.level > 1 && (this.header == null || this.header.level[this.level - 1].forward == null)) {
                    this.level--;
                }
                this.length--;
            }
        }
    }

    @Override
    public long getRank(E ele) {
        SkipListNode<E> x = this.header;
        if (x == null) {
            return 0;
        }
        long rank = 0;
        SkipListNode<E> forward = null;
        for (int i = this.level - 1; i >= 0; i--) {
            forward = x.level[i].forward;
            while (forward != null && cmp(forward.ele, ele) <= 0) {
                rank += x.level[i].span;
                x = forward;
                forward = x.level[i].forward;
            }
        }
        if (x.ele != null && cmp(x.ele, ele) == 0) {
            return rank;
        }
        return 0;
    }

    @Override
    public List<E> limit(int count) {
        List<E> result = new ArrayList<>(count);
        if (this.length == 0) {
            return result;
        }
        SkipListNode<E> forward = this.header.level[0].forward;
        int i = 0;
        while (i++ < count && forward != null) {
            result.add(forward.ele);
            forward = forward.level[0].forward;
        }
        return result;
    }

    @Override
    public void forEach(Consumer<E> consumer) {
        if (this.length == 0) {
            return;
        }
        SkipListNode<E> forward = this.header.level[0].forward;
        while (forward != null) {
            consumer.accept(forward.ele);
            forward = forward.level[0].forward;
        }
    }

    public int getLength() {
        return length;
    }

}
