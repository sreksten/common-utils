package com.threeamigos.common.util.implementations.collections;

import com.threeamigos.common.util.interfaces.collections.PriorityDeque;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;
import java.util.function.Function;

/**
 * Ultra-fast implementation of the {@link PriorityDeque} (small, fixed-range priorities).
 * Perfect when priorities are known and limited (range limit is 0..31).
 * Uses an array of Deque and a bitset to find the next non-empty priority in constant time.<br/>
 * <b>NOTE:</b> This implementation is not thread-safe. For that, wrap this class with
 * {@link SynchronizedPriorityDequeWrapper}.<br/>
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
 *
 * @author Stefano Reksten
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
        validateConstructorPriority(maxPriority);
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
    public void add(final @Nonnull T t, final int priority) {
        validateObject(t);
        validatePriority(priority);
        ArrayDeque<T> q = buckets[priority];
        q.addLast(t);
        nonEmptyMask |= (1 << priority);
    }

    @Override
    public @Nullable T peek() {
        return policy == Policy.FIFO ? peekFifo() : peekLifo();
    }

    @Override
    public @Nullable T peekFifo() {
        if (nonEmptyMask == 0) {
            return null;
        }
        return buckets[getHighestNotEmptyPriority()].peekFirst();
    }

    @Override
    public @Nullable T peekLifo() {
        if (nonEmptyMask == 0) {
            return null;
        }
        return buckets[getHighestNotEmptyPriority()].peekLast();
    }

    @Override
    public @Nullable T poll() {
        return policy == Policy.FIFO ? pollFifo() : pollLifo();
    }

    @Override
    public @Nullable T pollFifo() {
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
    public @Nullable T pollLifo() {
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
    public boolean isEmpty() {
        return nonEmptyMask == 0;
    }

    @Override
    public int size() {
        int size = 0;
        for (int i = 0; i <= maxPriority; i++) {
            size += buckets[i].size();
        }
        return size;
    }

    @Override
    public boolean contains(final @Nonnull T t) {
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
    public boolean containsAll(final @Nonnull Collection<T> iterable) {
        validateCollection(iterable);
        for (T t : iterable) {
            if (t == null) {
                return false;
            }
            boolean found = false;
            for (ArrayDeque<T> q : buckets) {
                if (q.contains(t)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void clear() {
        for (int i = 0; i <= maxPriority; i++) {
            buckets[i].clear();
        }
        nonEmptyMask = 0;
    }

    @Override
    public void clear(final @Nonnull Function<T, Boolean> filteringFunction) {
        validateFilteringFunction(filteringFunction);
        for (int i = 0; i <= maxPriority; i++) {
            buckets[i].removeIf(filteringFunction::apply);
            if (buckets[i].isEmpty()) {
                nonEmptyMask &= ~(1 << i);
            }
        }
    }

    @Override
    public boolean remove() {
        T t = poll();
        if (t == null) {
            throw new NoSuchElementException();
        }
        return true;
    }

    @Override
    public boolean remove(final @Nonnull T t) {
        if (t == null) {
            return false;
        }
        for (int i = 0; i <= maxPriority; i++) {
            if (buckets[i].remove(t)) {
                if (buckets[i].isEmpty()) {
                    nonEmptyMask &= ~(1 << i);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean removeAll(final @Nonnull Collection<T> iterable) {
        validateCollection(iterable);
        boolean result = false;
        for (T t : iterable) {
            if (remove(t)) {
                result = true;
            }
        }
        return result;
    }

    @Override
    public boolean retainAll(final @Nonnull Collection<T> iterable) {
        validateCollection(iterable);
        boolean result = false;
        for (int i = 0; i <= maxPriority; i++) {
            if (buckets[i].retainAll(iterable)) {
                result = true;
                if (buckets[i].isEmpty()) {
                    nonEmptyMask &= ~(1 << i);
                }
            }
        }
        return result;
    }

    @Override
    public @Nonnull List<T> toList() {
        List<T> result = new ArrayList<>();
        for (int i = maxPriority; i >= MIN_PRIORITY; i--) {
            ArrayDeque<T> bucket = buckets[i];
            if (bucket.isEmpty()) {
                continue;
            }
            if (policy == Policy.FIFO) {
                result.addAll(bucket);
            } else {
                bucket.descendingIterator().forEachRemaining(result::add);
            }
        }
        return result;
    }

    @Override
    public @Nonnull Iterator<T> iterator() {
        return new PriorityIterator();
    }

    @Override
    public int getHighestNotEmptyPriority() {
        if (nonEmptyMask == 0) return -1;
        return 31 - Integer.numberOfLeadingZeros(nonEmptyMask);
    }

    @Override
    public @Nullable T peek(final int priority) {
        validatePriority(priority);
        return policy == Policy.FIFO ? peekFifo(priority) : peekLifo(priority);
    }

    @Override
    public @Nullable T peekFifo(final int priority) {
        validatePriority(priority);
        ArrayDeque<T> q = buckets[priority];
        return q.peekFirst();
    }

    @Override
    public @Nullable T peekLifo(final int priority) {
        validatePriority(priority);
        ArrayDeque<T> q = buckets[priority];
        return q.peekLast();
    }

    @Override
    public @Nullable T poll(final int priority) {
        validatePriority(priority);
        return policy == Policy.FIFO ? pollFifo(priority) : pollLifo(priority);
    }

    @Override
    public @Nullable T pollFifo(final int priority) {
        validatePriority(priority);
        ArrayDeque<T> q = buckets[priority];
        T t = q.pollFirst();
        if (q.isEmpty()) {
            nonEmptyMask &= ~(1 << priority);
        }
        return t;
    }

    @Override
    public @Nullable T pollLifo(final int priority) {
        validatePriority(priority);
        ArrayDeque<T> q = buckets[priority];
        T t = q.pollLast();
        if (q.isEmpty()) {
            nonEmptyMask &= ~(1 << priority);
        }
        return t;
    }

    @Override
    public boolean isEmpty(final int priority) {
        validatePriority(priority);
        return buckets[priority].isEmpty();
    }

    @Override
    public int size(int priority) {
        validatePriority(priority);
        return buckets[priority].size();
    }

    @Override
    public boolean contains(final @Nonnull T t, final int priority) {
        validatePriority(priority);
        if (t == null) {
            return false;
        }
        ArrayDeque<T> q = buckets[priority];
        return q.contains(t);
    }

    @Override
    public boolean containsAll(final @Nonnull Collection<T> iterable, final int priority) {
        validateCollection(iterable);
        validatePriority(priority);
        ArrayDeque<T> q = buckets[priority];
        return q.containsAll(iterable);
    }

    @Override
    public void clear(final int priority) {
        validatePriority(priority);
        buckets[priority].clear();
        nonEmptyMask &= ~(1 << priority);
    }

    @Override
    public void clear(final @Nonnull Function<T, Boolean> filteringFunction, final int priority) {
        validateFilteringFunction(filteringFunction);
        validatePriority(priority);
        buckets[priority].removeIf(filteringFunction::apply);
        if (buckets[priority].isEmpty()) {
            nonEmptyMask &= ~(1 << priority);
        }
    }

    @Override
    public boolean remove(final int priority) {
        validatePriority(priority);
        T t = poll(priority);
        if (t == null) {
            throw new NoSuchElementException();
        }
        return true;
    }

    @Override
    public boolean remove(final @Nonnull T t, final int priority) {
        validatePriority(priority);
        if (t == null) {
            return false;
        }
        boolean removed = buckets[priority].remove(t);
        if (removed && buckets[priority].isEmpty()) {
            nonEmptyMask &= ~(1 << priority);
        }
        return removed;
    }

    @Override
    public boolean removeAll(final @Nonnull Collection<T> iterable, final int priority) {
        validateCollection(iterable);
        validatePriority(priority);
        boolean removed = buckets[priority].removeAll(iterable);
        if (removed && buckets[priority].isEmpty()) {
            nonEmptyMask &= ~(1 << priority);
        }
        return removed;
    }

    @Override
    public boolean retainAll(@Nonnull Collection<T> iterable, int priority) {
        validateCollection(iterable);
        validatePriority(priority);
        boolean changed = buckets[priority].retainAll(iterable);
        if (changed && buckets[priority].isEmpty()) {
            nonEmptyMask &= ~(1 << priority);
        }
        return changed;
    }

    @Override
    public @Nonnull List<T> toList(int priority) {
        validatePriority(priority);
        return new ArrayList<>(buckets[priority]);
    }

    @Override
    public @Nonnull Iterator<T> iterator(int priority) {
        validatePriority(priority);
        return new PriorityIterator(priority);
    }

    private class PriorityIterator implements Iterator<T> {
        private int currentBucketIndex;
        private Iterator<T> currentDequeIterator;
        private ArrayDeque<T> currentDeque;
        private boolean canRemove = false;

        PriorityIterator() {
            // bucket index will be decremented as the first thing
            this.currentBucketIndex = maxPriority + 1;
        }

        PriorityIterator(int fixedPriority) {
            // bucket index will be decremented as the first thing
            this.currentBucketIndex = fixedPriority + 1;
        }

        @Override
        public boolean hasNext() {
            while (currentDequeIterator == null || !currentDequeIterator.hasNext()) {
                currentBucketIndex--;
                if (currentBucketIndex < 0) {
                    return false;
                }
                currentDeque = buckets[currentBucketIndex];
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
            canRemove = true;
            return currentDequeIterator.next();
        }

        @Override
        public void remove() {
            if (!canRemove || currentDequeIterator == null) {
                throw new IllegalStateException();
            }
            currentDequeIterator.remove();
            canRemove = false;
            if (currentDeque.isEmpty()) {
                nonEmptyMask &= ~(1 << currentBucketIndex);
            }
        }
    }

    private void validatePolicy(@Nonnull Policy policy) {
        if (policy == null) {
            throw new IllegalArgumentException("Policy cannot be null");
        }
    }

    private void validateConstructorPriority(int priority) {
        if (priority < MIN_PRIORITY) {
            throw new IllegalArgumentException("Priority must be non-negative, got: " + priority);
        }
        if (priority > MAX_PRIORITY) {
            throw new IllegalArgumentException("Priority must be between " + MIN_PRIORITY + " and " + MAX_PRIORITY + ", got: " + priority);
        }
    }

    private void validatePriority(int priority) {
        if (priority < MIN_PRIORITY) {
            throw new IllegalArgumentException("Priority must be non-negative, got: " + priority);
        }
        if (priority > maxPriority) {
            throw new IllegalArgumentException("Priority must be between " + MIN_PRIORITY + " and " + maxPriority + ", got: " + priority);
        }
    }

    void validateObject(T object) {
        if (object == null) {
            throw new IllegalArgumentException("Object cannot be null");
        }
    }

    void validateCollection(Collection<T> collection) {
        if (collection == null) {
            throw new IllegalArgumentException("Collection cannot be null");
        }
    }

    void validateFilteringFunction(Function<T, Boolean> filteringFunction) {
        if (filteringFunction == null) {
            throw new IllegalArgumentException("Filtering function cannot be null");
        }
    }
}
