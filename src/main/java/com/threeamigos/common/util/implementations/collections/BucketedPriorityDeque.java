package com.threeamigos.common.util.implementations.collections;

import com.threeamigos.common.util.interfaces.collections.PriorityDeque;
import jakarta.annotation.Nonnull;

import java.util.*;
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
    private final int maxPriority; // inclusive

    private Policy policy;
    private int nonEmptyMask = 0; // bit i set => bucket i has items

    @SuppressWarnings("unchecked")
    public BucketedPriorityDeque(final int maxPriority, final @Nonnull Policy policy) {
        validatePriority(maxPriority);
        validatePolicy(policy);
        this.maxPriority = maxPriority;
        this.buckets = new ArrayDeque[maxPriority + 1];
        for (int i = 0; i <= maxPriority; i++) {
            buckets[i] = new ArrayDeque<>();
        }
        this.policy = policy;
    }

    public BucketedPriorityDeque(final int maxPriority) {
        this(maxPriority, Policy.FIFO);
    }

    public BucketedPriorityDeque() {
        this(MAX_PRIORITY);
    }

    public void setPolicy(@Nonnull Policy policy) {
        validatePolicy(policy);
        this.policy = policy;
    }

    public Policy getPolicy() {
        return policy;
    }

    public synchronized void add(@Nonnull T task, int priority) {
        if (task == null) {
            throw new NullPointerException("Task cannot be null");
        }
        validatePriority(priority);
        ArrayDeque<T> q = buckets[priority];
        q.addLast(task);
        nonEmptyMask |= (1 << priority);
    }

    public synchronized T peek() {
        return policy == Policy.FIFO ? peekFifo() : peekLifo();
    }

    public synchronized T peekFifo() {
        if (nonEmptyMask == 0) {
            return null;
        }
        return buckets[getHighestNotEmptyPriority()].peekFirst();
    }

    public synchronized T peekLifo() {
        if (nonEmptyMask == 0) {
            return null;
        }
        return buckets[getHighestNotEmptyPriority()].peekLast();
    }

    public synchronized T poll() {
        if (policy == Policy.FIFO) {
            return pollFifo();
        } else {
            return pollLifo();
        }
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

    public synchronized boolean isEmpty(final int priority) {
        return buckets[priority].isEmpty();
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

    public synchronized void clear(@Nonnull Function<T, Boolean> filteringFunction) {
        for (int i = 0; i <= maxPriority; i++) {
            buckets[i].removeIf(filteringFunction::apply);
        }
    }

    public synchronized int getHighestNotEmptyPriority() {
        if (nonEmptyMask == 0) return -1;
        return 31 - Integer.numberOfLeadingZeros(nonEmptyMask);
    }

    @Override
    public synchronized boolean contains(@Nonnull T t) {
        for (ArrayDeque<T> q : buckets) {
            if (q.contains(t)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized boolean containsAll(@Nonnull Collection<T> iterable) {
        for (T t : iterable) {
            if (!contains(t)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public synchronized boolean remove() {
        T t = poll();
        if (t == null) {
            throw new NoSuchElementException();
        }
        return true;
    }

    @Override
    public synchronized boolean remove(@Nonnull T t) {
        for (ArrayDeque<T> q : buckets) {
            if (q.remove(t)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized boolean removeAll(@Nonnull Collection<T> iterable) {
        boolean result = true;
        for (T t : iterable) {
            if (!remove(t)) {
                result = false;
            }
        }
        return result;
    }

    @Override
    public synchronized boolean retainAll(@Nonnull Collection<T> iterable) {
        boolean result = false;
        for (int i = 0; i <= maxPriority; i++) {
            if (buckets[i].retainAll(iterable)) {
                result = true;
            }
        }
        return result;
    }

    @Nonnull
    public synchronized List<T> toList() {
        List<T> result = new ArrayList<>();
        // Iterate from maxPriority down to MIN_PRIORITY (highest first)
        for (int i = maxPriority; i >= MIN_PRIORITY; i--) {
            ArrayDeque<T> bucket = buckets[i];
            if (bucket.isEmpty()) {
                continue;
            }
            if (policy == Policy.FIFO) {
                // FIFO: elements in the order they were added
                result.addAll(bucket);
            } else {
                // LIFO: elements in reverse order of addition
                bucket.descendingIterator().forEachRemaining(result::add);
            }
        }
        return result;
    }

    @Nonnull
    @Override
    public synchronized Iterator<T> iterator() {
        return new PriorityIterator();
    }

    private class PriorityIterator implements Iterator<T> {
        private int currentBucketIndex;
        private Iterator<T> currentDequeIterator;
        private ArrayDeque<T> currentDeque;

        PriorityIterator() {
            // Start from the highest possible priority
            this.currentBucketIndex = maxPriority;
        }

        @Override
        public boolean hasNext() {
            // If we don't have an iterator or the current one is exhausted
            while (currentDequeIterator == null || !currentDequeIterator.hasNext()) {
                currentBucketIndex--; // Move to the next bucket index

                if (currentBucketIndex < 0) {
                    return false; // No more buckets to check
                }

                currentDeque = buckets[currentBucketIndex];
                // Only create an iterator if the bucket actually has items
                if (!currentDeque.isEmpty()) {
                    currentDequeIterator = (policy == Policy.FIFO)
                            ? currentDeque.iterator()
                            : currentDeque.descendingIterator();
                }
            }
            return true;
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return currentDequeIterator.next();
        }

        @Override
        public void remove() {
            if (currentDequeIterator == null) {
                throw new IllegalStateException();
            }

            synchronized (BucketedPriorityDeque.this) {
                currentDequeIterator.remove();
                if (currentDeque.isEmpty()) {
                    // Update the bitmask if the bucket is now empty
                    nonEmptyMask &= ~(1 << currentBucketIndex);
                }
            }
        }
    }

    private void validatePolicy(@Nonnull Policy policy) {
        if (policy == null) {
            throw new IllegalArgumentException("Policy cannot be null");
        }
    }

    private void validatePriority(int maxPriority) {
        if (maxPriority < MIN_PRIORITY) {
            throw new IllegalArgumentException("maxPriority must be non-negative");
        }
        if (maxPriority > MAX_PRIORITY) {
            throw new IllegalArgumentException("maxPriority must be <= " + MAX_PRIORITY);
        }
    }

}
