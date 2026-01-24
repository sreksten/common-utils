package com.threeamigos.common.util.implementations.collections;

import com.threeamigos.common.util.interfaces.collections.PriorityDeque;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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

    public void setPolicy(final @Nonnull Policy policy) {
        validatePolicy(policy);
        this.policy = policy;
    }

    public Policy getPolicy() {
        return policy;
    }

    @Override
    public synchronized void add(final @Nonnull T t, final int priority) {
        validateObject(t);
        validatePriority(priority);
        ArrayDeque<T> q = buckets[priority];
        q.addLast(t);
        nonEmptyMask |= (1 << priority);
    }

    @Override
    public synchronized T peek() {
        return policy == Policy.FIFO ? peekFifo() : peekLifo();
    }

    @Override
    public synchronized T peekFifo() {
        if (nonEmptyMask == 0) {
            return null;
        }
        return buckets[getHighestNotEmptyPriority()].peekFirst();
    }

    @Override
    public synchronized T peekLifo() {
        if (nonEmptyMask == 0) {
            return null;
        }
        return buckets[getHighestNotEmptyPriority()].peekLast();
    }

    @Override
    public synchronized T poll() {
        if (policy == Policy.FIFO) {
            return pollFifo();
        } else {
            return pollLifo();
        }
    }

    @Override
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

    @Override
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

    @Override
    public synchronized boolean isEmpty() {
        return nonEmptyMask == 0;
    }

    @Override
    public synchronized int size() {
        int size = 0;
        for (int i = 0; i <= maxPriority; i++) {
            size += buckets[i].size();
        }
        return size;
    }

    @Override
    public synchronized boolean contains(final @Nonnull T t) {
        if (t == null) {
            return false;
        }
        for (ArrayDeque<T> q : buckets) {
            if (q.contains(t)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized boolean containsAll(final @Nonnull Collection<T> iterable) {
        validateCollection(iterable);
        for (T t : iterable) {
            if (!contains(t)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public synchronized void clear() {
        for (int i = 0; i <= maxPriority; i++) {
            buckets[i].clear();
        }
        nonEmptyMask = 0;
    }

    @Override
    public synchronized void clear(final @Nonnull Function<T, Boolean> filteringFunction) {
        validateFilteringFunction(filteringFunction);
        for (int i = 0; i <= maxPriority; i++) {
            buckets[i].removeIf(filteringFunction::apply);
        }
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
    public synchronized boolean remove(final @Nonnull T t) {
        if (t != null) {
            for (ArrayDeque<T> q : buckets) {
                if (q.remove(t)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public synchronized boolean removeAll(final @Nonnull Collection<T> iterable) {
        validateCollection(iterable);
        boolean result = true;
        for (T t : iterable) {
            if (!remove(t)) {
                result = false;
            }
        }
        return result;
    }

    @Override
    public synchronized boolean retainAll(final @Nonnull Collection<T> iterable) {
        validateCollection(iterable);
        boolean result = false;
        for (int i = 0; i <= maxPriority; i++) {
            if (buckets[i].retainAll(iterable)) {
                result = true;
            }
        }
        return result;
    }

    @Override
    public synchronized @Nonnull List<T> toList() {
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

    @Override
    public synchronized @Nonnull Iterator<T> iterator() {
        return new PriorityIterator();
    }

    @Override
    public synchronized int getHighestNotEmptyPriority() {
        if (nonEmptyMask == 0) return -1;
        return 31 - Integer.numberOfLeadingZeros(nonEmptyMask);
    }

    @Override
    public @Nullable T peek(final int priority) {
        ArrayDeque<T> q = buckets[priority];
        return q.peekFirst();
    }

    @Override
    public @Nullable T peekFifo(final int priority) {
        ArrayDeque<T> q = buckets[priority];
        return q.peekFirst();
    }

    @Override
    public @Nullable T peekLifo(final int priority) {
        ArrayDeque<T> q = buckets[priority];
        return q.peekLast();
    }

    @Override
    public @Nullable T poll(final int priority) {
        if (policy == Policy.FIFO) {
            return pollFifo(priority);
        } else {
            return pollLifo(priority);
        }
    }

    @Override
    public synchronized T pollFifo(final int priority) {
        ArrayDeque<T> q = buckets[priority];
        T t = q.pollFirst();
        if (q.isEmpty()) {
            nonEmptyMask &= ~(1 << priority);
        }
        return t;
    }

    @Override
    public synchronized T pollLifo(final int priority) {
        ArrayDeque<T> q = buckets[priority];
        T t = q.pollLast();
        if (q.isEmpty()) {
            nonEmptyMask &= ~(1 << priority);
        }
        return t;
    }

    @Override
    public synchronized boolean isEmpty(final int priority) {
        return buckets[priority].isEmpty();
    }

    @Override
    public synchronized int size(int priority) {
        return buckets[priority].size();
    }

    @Override
    public boolean contains(final @Nonnull T t, final int priority) {
        if (t == null) {
            return false;
        }
        ArrayDeque<T> q = buckets[priority];
        return q.contains(t);
    }

    @Override
    public boolean containsAll(final @Nonnull Collection<T> iterable, final int priority) {
        validateCollection(iterable);
        ArrayDeque<T> q = buckets[priority];
        return q.containsAll(iterable);
    }

    @Override
    public synchronized void clear(final int priority) {
        buckets[priority].clear();
    }

    @Override
    public synchronized void clear(final @Nonnull Function<T, Boolean> filteringFunction, final int priority) {
        validateFilteringFunction(filteringFunction);
        buckets[priority].removeIf(filteringFunction::apply);
    }

    @Override
    public boolean remove(final int priority) {
        T t = poll(priority);
        if (t == null) {
            throw new NoSuchElementException();
        }
        return true;
    }

    @Override
    public boolean remove(final @Nonnull T t, final int priority) {
        if (t == null) {
            return false;
        }
        return buckets[priority].remove(t);
    }

    @Override
    public boolean removeAll(final @Nonnull Collection<T> iterable, final int priority) {
        validateCollection(iterable);
        return buckets[priority].removeAll(iterable);
    }

    @Override
    public boolean retainAll(@Nonnull Collection<T> iterable, int priority) {
        validateCollection(iterable);
        return buckets[priority].retainAll(iterable);
    }

    @Override
    public @Nonnull List<T> toList(int priority) {
        return new ArrayList<>(buckets[priority]);
    }

    @Override
    public @Nonnull Iterator<T> iterator(int priority) {
        return buckets[priority].iterator();
    }

    private class PriorityIterator implements Iterator<T> {
        private int currentBucketIndex;
        private Iterator<T> currentDequeIterator;
        private ArrayDeque<T> currentDeque;

        PriorityIterator() {
            // Start from the highest possible priority
            this.currentBucketIndex = maxPriority + 1;
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

    void validateObject(T object) {
        if (object == null) {
            throw new NullPointerException("Object cannot be null");
        }
    }

    void validateCollection(Collection<T> collection) {
        if (collection == null) {
            throw new NullPointerException("Collection cannot be null");
        }
    }

    void validateFilteringFunction(Function<T, Boolean> filteringFunction) {
        if (filteringFunction == null) {
            throw new IllegalArgumentException("Filtering function cannot be null");
        }
    }
}
