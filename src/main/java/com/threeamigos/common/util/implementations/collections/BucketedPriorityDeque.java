package com.threeamigos.common.util.implementations.collections;

import com.threeamigos.common.util.interfaces.collections.PriorityDeque;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
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

    private final ReentrantReadWriteLock rw = new ReentrantReadWriteLock();
    private final Lock read = rw.readLock();
    private final Lock write = rw.writeLock();

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
        write.lock();
        try {
            this.policy = policy;
        } finally {
            write.unlock();
        }
    }

    public Policy getPolicy() {
        read.lock();
        try {
            return policy;
        } finally {
            read.unlock();
        }
    }

    @Override
    public void add(final @Nonnull T t, final int priority) {
        validateObject(t);
        validatePriority(priority);
        write.lock();
        try {
            ArrayDeque<T> q = buckets[priority];
            q.addLast(t);
            nonEmptyMask |= (1 << priority);
        } finally {
            write.unlock();
        }
    }

    @Override
    public T peek() {
        read.lock();
        try {
            return policy == Policy.FIFO ? peekFifo() : peekLifo();
        } finally {
            read.unlock();
        }
    }

    @Override
    public T peekFifo() {
        read.lock();
        try {
            if (nonEmptyMask == 0) {
                return null;
            }
            return buckets[getHighestNotEmptyPriority()].peekFirst();
        } finally {
            read.unlock();
        }
    }

    @Override
    public T peekLifo() {
        read.lock();
        try {
            if (nonEmptyMask == 0) {
                return null;
            }
            return buckets[getHighestNotEmptyPriority()].peekLast();
        } finally {
            read.unlock();
        }
    }

    @Override
    public T poll() {
        write.lock();
        try {
            return policy == Policy.FIFO ? pollFifo() : pollLifo();
        } finally {
            write.unlock();
        }
    }

    @Override
    public T pollFifo() {
        write.lock();
        try {
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
        } finally {
            write.unlock();
        }
    }

    @Override
    public T pollLifo() {
        write.lock();
        try {
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
        } finally {
            write.unlock();
        }
    }

    @Override
    public boolean isEmpty() {
        read.lock();
        try {
            return nonEmptyMask == 0;
        } finally {
            read.unlock();
        }
    }

    @Override
    public int size() {
        read.lock();
        try {
            int size = 0;
            for (int i = 0; i <= maxPriority; i++) {
                size += buckets[i].size();
            }
            return size;
        } finally {
            read.unlock();
        }
    }

    @Override
    public boolean contains(final @Nonnull T t) {
        read.lock();
        try {
            if (t == null) {
                return false;
            }
            for (ArrayDeque<T> q : buckets) {
                if (q.contains(t)) {
                    return true;
                }
            }
            return false;
        } finally {
            read.unlock();
        }
    }

    @Override
    public boolean containsAll(final @Nonnull Collection<T> iterable) {
        validateCollection(iterable);
        read.lock();
        try {
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
        } finally {
            read.unlock();
        }
    }

    @Override
    public void clear() {
        write.lock();
        try {
            for (int i = 0; i <= maxPriority; i++) {
                buckets[i].clear();
            }
            nonEmptyMask = 0;
        } finally {
            write.unlock();
        }
    }

    @Override
    public void clear(final @Nonnull Function<T, Boolean> filteringFunction) {
        validateFilteringFunction(filteringFunction);
        write.lock();
        try {
            for (int i = 0; i <= maxPriority; i++) {
                buckets[i].removeIf(filteringFunction::apply);
                if (buckets[i].isEmpty()) {
                    nonEmptyMask &= ~(1 << i);
                }
            }
        } finally {
            write.unlock();
        }
    }

    @Override
    public boolean remove() {
        write.lock();
        try {
            T t = poll();
            if (t == null) {
                throw new NoSuchElementException();
            }
            return true;
        } finally {
            write.unlock();
        }
    }

    @Override
    public boolean remove(final @Nonnull T t) {
        if (t == null) {
            return false;
        }
        write.lock();
        try {
            for (int i = 0; i <= maxPriority; i++) {
                if (buckets[i].remove(t)) {
                    if (buckets[i].isEmpty()) {
                        nonEmptyMask &= ~(1 << i);
                    }
                    return true;
                }
            }
            return false;
        } finally {
            write.unlock();
        }
    }

    @Override
    public boolean removeAll(final @Nonnull Collection<T> iterable) {
        validateCollection(iterable);
        write.lock();
        try {
            boolean result = true;
            for (T t : iterable) {
                if (!remove(t)) {
                    result = false;
                }
            }
            return result;
        } finally {
            write.unlock();
        }
    }

    @Override
    public boolean retainAll(final @Nonnull Collection<T> iterable) {
        validateCollection(iterable);
        write.lock();
        try {
            boolean result = false;
            for (int i = 0; i <= maxPriority; i++) {
                if (buckets[i].retainAll(iterable)) {
                    result = true;
                }
            }
            return result;
        } finally {
            write.unlock();
        }
    }

    @Override
    public @Nonnull List<T> toList() {
        read.lock();
        try {
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
        } finally {
            read.unlock();
        }
    }

    @Override
    public @Nonnull Iterator<T> iterator() {
        return new PriorityIterator();
    }

    @Override
    public int getHighestNotEmptyPriority() {
        read.lock();
        try {
            if (nonEmptyMask == 0) return -1;
            return 31 - Integer.numberOfLeadingZeros(nonEmptyMask);
        } finally {
            read.unlock();
        }
    }

    @Override
    public @Nullable T peek(final int priority) {
        validatePriority(priority);
        read.lock();
        try {
            ArrayDeque<T> q = buckets[priority];
            return q.peekFirst();
        } finally {
            read.unlock();
        }
    }

    @Override
    public @Nullable T peekFifo(final int priority) {
        validatePriority(priority);
        read.lock();
        try {
            ArrayDeque<T> q = buckets[priority];
            return q.peekFirst();
        } finally {
            read.unlock();
        }
    }

    @Override
    public @Nullable T peekLifo(final int priority) {
        validatePriority(priority);
        read.lock();
        try {
            ArrayDeque<T> q = buckets[priority];
            return q.peekLast();
        } finally {
            read.unlock();
        }
    }

    @Override
    public @Nullable T poll(final int priority) {
        validatePriority(priority);
        write.lock();
        try {
            return policy == Policy.FIFO ? pollFifo(priority) : pollLifo(priority);
        } finally {
            write.unlock();
        }
    }

    @Override
    public T pollFifo(final int priority) {
        validatePriority(priority);
        write.lock();
        try {
            ArrayDeque<T> q = buckets[priority];
            T t = q.pollFirst();
            if (q.isEmpty()) {
                nonEmptyMask &= ~(1 << priority);
            }
            return t;
        } finally {
            write.unlock();
        }
    }

    @Override
    public T pollLifo(final int priority) {
        validatePriority(priority);
        write.lock();
        try {
            ArrayDeque<T> q = buckets[priority];
            T t = q.pollLast();
            if (q.isEmpty()) {
                nonEmptyMask &= ~(1 << priority);
            }
            return t;
        } finally {
            write.unlock();
        }
    }

    @Override
    public boolean isEmpty(final int priority) {
        validatePriority(priority);
        read.lock();
        try {
            return buckets[priority].isEmpty();
        } finally {
            read.unlock();
        }
    }

    @Override
    public int size(int priority) {
        validatePriority(priority);
        read.lock();
        try {
            return buckets[priority].size();
        } finally {
            read.unlock();
        }
    }

    @Override
    public boolean contains(final @Nonnull T t, final int priority) {
        validatePriority(priority);
        read.lock();
        try {
            if (t == null) {
                return false;
            }
            ArrayDeque<T> q = buckets[priority];
            return q.contains(t);
        } finally {
            read.unlock();
        }
    }

    @Override
    public boolean containsAll(final @Nonnull Collection<T> iterable, final int priority) {
        validateCollection(iterable);
        validatePriority(priority);
        read.lock();
        try {
            ArrayDeque<T> q = buckets[priority];
            return q.containsAll(iterable);
        } finally {
            read.unlock();
        }
    }

    @Override
    public void clear(final int priority) {
        validatePriority(priority);
        write.lock();
        try {
            buckets[priority].clear();
            nonEmptyMask &= ~(1 << priority);
        } finally {
            write.unlock();
        }
    }

    @Override
    public void clear(final @Nonnull Function<T, Boolean> filteringFunction, final int priority) {
        validateFilteringFunction(filteringFunction);
        validatePriority(priority);
        write.lock();
        try {
            buckets[priority].removeIf(filteringFunction::apply);
            if (buckets[priority].isEmpty()) {
                nonEmptyMask &= ~(1 << priority);
            }
        } finally {
            write.unlock();
        }
    }

    @Override
    public boolean remove(final int priority) {
        validatePriority(priority);
        write.lock();
        try {
            T t = poll(priority);
            if (t == null) {
                throw new NoSuchElementException();
            }
            return true;
        } finally {
            write.unlock();
        }
    }

    @Override
    public boolean remove(final @Nonnull T t, final int priority) {
        validatePriority(priority);
        write.lock();
        try {
            if (t == null) {
                return false;
            }
            boolean removed = buckets[priority].remove(t);
            if (removed && buckets[priority].isEmpty()) {
                nonEmptyMask &= ~(1 << priority);
            }
            return removed;
        } finally {
            write.unlock();
        }
    }

    @Override
    public boolean removeAll(final @Nonnull Collection<T> iterable, final int priority) {
        validateCollection(iterable);
        validatePriority(priority);
        write.lock();
        try {
            boolean removed = buckets[priority].removeAll(iterable);
            if (removed && buckets[priority].isEmpty()) {
                nonEmptyMask &= ~(1 << priority);
            }
            return removed;
        } finally {
            write.unlock();
        }
    }

    @Override
    public boolean retainAll(@Nonnull Collection<T> iterable, int priority) {
        validateCollection(iterable);
        validatePriority(priority);
        write.lock();
        try {
            boolean changed = buckets[priority].retainAll(iterable);
            if (changed && buckets[priority].isEmpty()) {
                nonEmptyMask &= ~(1 << priority);
            }
            return changed;
        } finally {
            write.unlock();
        }
    }

    @Override
    public @Nonnull List<T> toList(int priority) {
        validatePriority(priority);
        read.lock();
        try {
            return new ArrayList<>(buckets[priority]);
        } finally {
            read.unlock();
        }
    }

    @Override
    public @Nonnull Iterator<T> iterator(int priority) {
        validatePriority(priority);
        // Iterator will hold write lock for strong consistency + remove support.
        return new PriorityIterator(priority);
    }

    private class PriorityIterator implements Iterator<T> {
        private int currentBucketIndex;
        private Iterator<T> currentDequeIterator;
        private ArrayDeque<T> currentDeque;
        private boolean lockHeld;
        private boolean canRemove = false;

        PriorityIterator() {
            // Hold write lock for entire iteration to allow safe remove.
            write.lock();
            lockHeld = true;
            this.currentBucketIndex = maxPriority + 1;
        }

        PriorityIterator(int fixedPriority) {
            write.lock();
            lockHeld = true;
            this.currentBucketIndex = fixedPriority + 1;
        }

        private void releaseWriteLockIfHeld() {
            if (lockHeld) {
                write.unlock();
                lockHeld = false;
            }
        }

        @Override
        public boolean hasNext() {
            try {
                while (currentDequeIterator == null || !currentDequeIterator.hasNext()) {
                    currentBucketIndex--;
                    if (currentBucketIndex < 0) {
                        releaseWriteLockIfHeld();
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
            } catch (Exception e) {
                releaseWriteLockIfHeld();
                throw e;
            }
        }

        @Override
        public T next() {
            try {
                if (!hasNext()) {
                    releaseWriteLockIfHeld();
                    throw new NoSuchElementException();
                }
                canRemove = true;
                return currentDequeIterator.next();
            } catch (Exception e) {
                releaseWriteLockIfHeld();
                throw e;
            }
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