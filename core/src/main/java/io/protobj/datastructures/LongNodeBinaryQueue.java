package io.protobj.datastructures;

import java.util.Arrays;

public class LongNodeBinaryQueue<T extends LongNodeBinaryQueue.Node> {
    /**
     * Default initial capacity.
     */
    private static final int DEFAULT_CAPACITY = 16;


    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;


    public int size;

    private Node[] nodes;
    private final boolean isMaxHeap;

    public LongNodeBinaryQueue() {
        this(0, false);
    }

    public LongNodeBinaryQueue(int capacity, boolean isMaxHeap) {
        this.isMaxHeap = isMaxHeap;
        nodes = new Node[Math.max(DEFAULT_CAPACITY, capacity)];
    }


    private void ensureExplicitCapacity(int minCapacity) {
        // overflow-conscious code
        if (minCapacity - nodes.length > 0)
            grow(minCapacity);
    }

    private void grow(int minCapacity) {
        // overflow-conscious code
        int oldCapacity = nodes.length;
        int newCapacity = oldCapacity + (oldCapacity >> 1);
        if (newCapacity - minCapacity < 0)
            newCapacity = minCapacity;
        if (newCapacity - MAX_ARRAY_SIZE > 0)
            newCapacity = hugeCapacity(minCapacity);
        // minCapacity is usually close to size, so this is a win:
        nodes = Arrays.copyOf(nodes, newCapacity);
    }

    private static int hugeCapacity(int minCapacity) {
        if (minCapacity < 0) // overflow
            throw new OutOfMemoryError();
        return (minCapacity > MAX_ARRAY_SIZE) ?
                Integer.MAX_VALUE :
                MAX_ARRAY_SIZE;
    }

    /**
     * 添加节点
     *
     * @param node
     * @return
     */
    public T add(T node) {
        // Expand if necessary.
        ensureExplicitCapacity(size + 1);
        // Insert at end and bubble up.
        node.index = size;
        nodes[size] = node;
        up(size++);
        return node;
    }

    /**
     * 添加节点，并设置排序比较值
     *
     * @param node
     * @param value 排序比较值
     * @return
     */
    public T add(T node, long value) {
        node.value = value;
        return add(node);
    }

    @SuppressWarnings("unchecked")
    public T peek() {
        if (size == 0)
            return null;
        return (T) nodes[0];
    }

    /**
     * 获得堆最小值，并移除
     *
     * @return
     */
    public T pop() {
        return remove(0);
    }

    public T remove(T node) {
        return remove(node.index);
    }

    /**
     * 移除元素
     *
     * @param index
     * @return
     */
    @SuppressWarnings("unchecked")
    private T remove(int index) {
        Node[] nodes = this.nodes;
        Node removed = nodes[index];
        nodes[index] = nodes[--size];
        nodes[size] = null;
        if (size > 0 && index < size)
            down(index);
        return (T) removed;
    }

    public void clear() {
        Node[] nodes = this.nodes;
        for (int i = 0, n = size; i < n; i++)
            nodes[i] = null;
        size = 0;
    }

    /**
     * 设置节点值，并进行排序
     *
     * @param node
     * @param value
     */
    public void setValue(T node, long value) {
        long oldValue = node.value;
        node.value = value;
        if (value < oldValue ^ isMaxHeap)
            up(node.index);
        else
            down(node.index);
    }

    /**
     * 如果节点值小于中间节点值，将节点上移，否则放在末尾
     *
     * @param index
     */
    private void up(int index) {
        Node[] nodes = this.nodes;
        Node node = nodes[index];
        long value = node.value;
        while (index > 0) {
            int parentIndex = (index - 1) >> 1;
            Node parent = nodes[parentIndex];
            if (value < parent.value ^ isMaxHeap) {
                nodes[index] = parent;
                parent.index = index;
                index = parentIndex;
            } else
                break;
        }
        nodes[index] = node;
        node.index = index;
    }

    /**
     * 节点后移
     *
     * @param index
     */
    private void down(int index) {
        Node[] nodes = this.nodes;
        int size = this.size;

        Node node = nodes[index];
        long value = node.value;

        while (true) {
            int leftIndex = 1 + (index << 1);
            if (leftIndex >= size)
                break;
            int rightIndex = leftIndex + 1;

            // Always have a left child.
            Node leftNode = nodes[leftIndex];
            long leftValue = leftNode.value;

            // May have a right child.
            Node rightNode;
            long rightValue;
            if (rightIndex >= size) {
                rightNode = null;
                rightValue = isMaxHeap ? Long.MIN_VALUE : Long.MAX_VALUE;
            } else {
                rightNode = nodes[rightIndex];
                rightValue = rightNode.value;
            }

            // The smallest of the three values is the parent.
            if (leftValue < rightValue ^ isMaxHeap) {
                if (leftValue == value || (leftValue > value ^ isMaxHeap))
                    break;
                nodes[index] = leftNode;
                leftNode.index = index;
                index = leftIndex;
            } else {
                if (rightValue == value || (rightValue > value ^ isMaxHeap))
                    break;
                nodes[index] = rightNode;
                rightNode.index = index;
                index = rightIndex;
            }
        }

        nodes[index] = node;
        node.index = index;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof LongNodeBinaryQueue))
            return false;
        LongNodeBinaryQueue other = (LongNodeBinaryQueue) obj;
        if (other.size != size)
            return false;
        for (int i = 0, n = size; i < n; i++)
            if (other.nodes[i].value != nodes[i].value)
                return false;
        return true;
    }

    public int hashCode() {
        int h = 1;
        for (int i = 0, n = size; i < n; i++)
            h = h * 31 + Long.hashCode(nodes[i].value);
        return h;
    }

    public String toString() {
        if (size == 0)
            return "[]";
        Node[] nodes = this.nodes;
        StringBuilder buffer = new StringBuilder(32);
        buffer.append('[');
        buffer.append(nodes[0].value);
        for (int i = 1; i < size; i++) {
            buffer.append(", ");
            buffer.append(nodes[i].value);
        }
        buffer.append(']');
        return buffer.toString();
    }

    /**
     * @author Nathan Sweet
     */
    static public class Node {
        long value; //节点排序比较值
        int index;    //节点索引

        public Node(long value) {
            this.value = value;
        }

        public long getValue() {
            return value;
        }

        public void setValue(long value) {
            this.value = value;
        }

        public String toString() {
            return Long.toString(value);
        }
    }

}
