package com.threeamigos.common.util.implementations.collections;

import com.threeamigos.common.util.interfaces.collections.PriorityDeque;
import java.util.ArrayDeque;
import java.util.function.Function;

/**
 * Ultra-fast, thread-safe implementation of the {@link PriorityDeque} (small, fixed-range priorities).
 * Perfect when priorities are known and limited (range limit is 0..31).
 * Uses an array of Deque and a bitset to find the next non-empty priority in constant time.<br/>
 * <br/>
 * Complexity:
 * <ul>
 * <li>add: O(1)</li>
 * <li>poll*: O(1) to find the highest non-empty bucket + O(1) to pop</li>
 * </ul>
 * Switch between FIFO and LIFO on the fly by calling the respective poll* method—no data rebuild required.<br/>
 * Use this variant if:
 * <ul>
 * <li>Priorities are known and limited (range limit is 0..31).</li>
 * <li>You need maximum throughput with predictable constant-time ops.</li>
 * </ul>
 *
 * @param <T> type of the objects stored in the deque
 */
public class BucketedPriorityDeque<T> implements PriorityDeque<T> {

    public static final int MIN_PRIORITY = 0;
    public static final int MAX_PRIORITY = 31;

    private final ArrayDeque<T>[] buckets;
    private int nonEmptyMask = 0; // bit i set => bucket i has items
    private final int maxPriority; // inclusive

    @SuppressWarnings("unchecked")
    public BucketedPriorityDeque(int maxPriority) {
        if (maxPriority < MIN_PRIORITY) {
            throw new IllegalArgumentException("maxPriority must be non-negative");
        }
        if (maxPriority > MAX_PRIORITY) {
            throw new IllegalArgumentException("maxPriority must be <= " + MAX_PRIORITY);
        }
        this.maxPriority = maxPriority;
        this.buckets = new ArrayDeque[maxPriority + 1];
        for (int i = 0; i <= maxPriority; i++) {
            buckets[i] = new ArrayDeque<>();
        }
    }

    public synchronized int getHighestNotEmptyPriority() {
        if (nonEmptyMask == 0) return -1;
        return 31 - Integer.numberOfLeadingZeros(nonEmptyMask);
    }

    public synchronized void add(T task, int priority) {
        ArrayDeque<T> q = buckets[priority];
        q.addLast(task);
        nonEmptyMask |= (1 << priority);
    }

    public synchronized T pollFifo() {
        int p = getHighestNotEmptyPriority();
        if (p < 0) {
            return null;
        }
        ArrayDeque<T> q = buckets[p];
        T t = q.pollFirst();
        if (q.isEmpty()) {
            nonEmptyMask &= ~(1 << p);
        }
        return t;
    }

    public synchronized T pollFifo(int priority) {
        ArrayDeque<T> q = buckets[priority];
        T t = q.pollFirst();
        if (q.isEmpty()) {
            nonEmptyMask &= ~(1 << priority);
        }
        return t;
    }

    public synchronized T pollLifo() {
        int p = getHighestNotEmptyPriority();
        if (p < 0) {
            return null;
        }
        ArrayDeque<T> q = buckets[p];
        T t = q.pollLast();
        if (q.isEmpty()) {
            nonEmptyMask &= ~(1 << p);
        }
        return t;
    }

    public synchronized T pollLifo(int priority) {
        ArrayDeque<T> q = buckets[priority];
        T t = q.pollLast();
        if (q.isEmpty()) {
            nonEmptyMask &= ~(1 << priority);
        }
        return t;
    }

    public synchronized boolean isEmpty() {
        return nonEmptyMask == 0;
    }

    public synchronized int size() {
        int size = 0;
        for (int i = 0; i <= maxPriority; i++) {
            size += buckets[i].size();
        }
        return size;
    }

    public synchronized int size(int priority) {
        return buckets[priority].size();
    }

    public synchronized void clear() {
        for (int i = 0; i <= maxPriority; i++) {
            buckets[i].clear();
        }
        nonEmptyMask = 0;
    }

    public synchronized void clear(int priority) {
        buckets[priority].clear();
    }

    public synchronized void clear(Function<T, Boolean> filteringFunction) {
        for (int i = 0; i <= maxPriority; i++) {
            buckets[i].removeIf(filteringFunction::apply);
        }
    }
}
