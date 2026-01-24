package com.threeamigos.common.util.implementations.collections;

import com.threeamigos.common.util.interfaces.collections.PriorityDeque;
import jakarta.annotation.Nonnull;

import java.util.*;
import java.util.function.Function;

/**
 * General-purpose, thread-safe implementation of a {@link PriorityDeque} (arbitrary priority integers).
 * Works for any integer priorities, sparse ranges, dynamic inserts/removals.<br/>
 * <br/>
 * Complexity:
 * <ul>
 * <li>add: O(log P) to locate/insert the priority bucket (amortized O(1) per element inside the deque).</li>
 * <li>poll*: O(log P) to get the highest priority + O(1) to pop.</li>
 * </ul>
 * Switch between FIFO and LIFO on the fly by calling the respective poll* method—no data rebuild required.<br/>
 * Use this variant if:
 * <ul>
 * <li>Priorities are unbounded or sparse.</li>
 * <li>You may dynamically introduce many new priority levels.</li>
 * </ul>
 *
 * @param <T> type of the objects stored in the deque
 *
 * @author Stefano Reksten
 */
public class GeneralPurposePriorityDeque<T> implements PriorityDeque<T> {

    private final NavigableMap<Integer, ArrayDeque<T>> byPriority = new TreeMap<>();

    private Policy policy;
    private int nonEmptyCount = 0;

    public GeneralPurposePriorityDeque() {
        this.policy = Policy.FIFO;
    }

    public GeneralPurposePriorityDeque(final Policy policy) {
        this.policy = policy;
    }

    public void setPolicy(@Nonnull final Policy policy) {
        this.policy = policy;
    }

    public Policy getPolicy() {
        return policy;
    }

    public synchronized void add(@Nonnull final T task, final int priority) {
        ArrayDeque<T> q = byPriority.computeIfAbsent(priority, p -> {
            nonEmptyCount++;
            return new ArrayDeque<>();
        });
        q.addLast(task);
    }

    public synchronized T peek() {
        return policy == Policy.FIFO ? peekFifo() : peekLifo();
    }

    public synchronized T peekFifo() {
        Map.Entry<Integer, ArrayDeque<T>> integerArrayDequeEntry = byPriority.lastEntry();
        return integerArrayDequeEntry != null ? integerArrayDequeEntry.getValue().peekFirst() : null;
    }

    public synchronized T peekLifo() {
        Map.Entry<Integer, ArrayDeque<T>> integerArrayDequeEntry = byPriority.lastEntry();
        return integerArrayDequeEntry != null ? integerArrayDequeEntry.getValue().peekLast() : null;
    }

    public synchronized T poll() {
        if (policy == Policy.FIFO) {
            return pollFifo();
        } else {
            return pollLifo();
        }
    }

    /** Take the next task preferring the highest priority, FIFO within that priority */
    public synchronized T pollFifo() {
        Map.Entry<Integer, ArrayDeque<T>> e = byPriority.lastEntry();
        if (e == null) {
            return null;
        }
        ArrayDeque<T> q = e.getValue();
        T t = q.pollFirst();
        if (q.isEmpty()) {
            byPriority.remove(e.getKey());
            nonEmptyCount--;
        }
        return t;
    }

    public synchronized T pollFifo(final int priority) {
        ArrayDeque<T> q = byPriority.get(priority);
        if (q == null) {
            return null;
        }
        T t = q.pollFirst();
        if (q.isEmpty()) {
            byPriority.remove(priority);
            nonEmptyCount--;
        }
        return t;
    }

    /** Take the next task preferring the highest priority, LIFO within that priority */
    public synchronized T pollLifo() {
        Map.Entry<Integer, ArrayDeque<T>> e = byPriority.lastEntry();
        if (e == null) {
            return null;
        }
        ArrayDeque<T> q = e.getValue();
        T t = q.pollLast();
        if (q.isEmpty()) {
            byPriority.remove(e.getKey());
            nonEmptyCount--;
        }
        return t;
    }

    public synchronized T pollLifo(final int priority) {
        ArrayDeque<T> q = byPriority.get(priority);
        if (q == null) {
            return null;
        }
        T t = q.pollLast();
        if (q.isEmpty()) {
            byPriority.remove(priority);
            nonEmptyCount--;
        }
        return t;
    }

    public synchronized boolean isEmpty() {
        return nonEmptyCount == 0;
    }

    public synchronized boolean isEmpty(final int priority) {
        ArrayDeque<T> q = byPriority.get(priority);
        return q == null || q.isEmpty();
    }

    public synchronized int size() {
        return byPriority.values().stream().mapToInt(ArrayDeque::size).sum();
    }

    public synchronized int size(final int priority) {
        ArrayDeque<T> q = byPriority.get(priority);
        return q == null ? 0 : q.size();
    }

    public synchronized void clear() {
        byPriority.clear();
        nonEmptyCount = 0;
    }

    public synchronized void clear(final int priority) {
        byPriority.remove(priority);
    }

    public synchronized void clear(@Nonnull final Function<T, Boolean> filteringFunction) {
        byPriority.values().forEach(q -> q.removeIf(filteringFunction::apply));
        nonEmptyCount = byPriority.values().stream().mapToInt(ArrayDeque::size).sum();
    }

    public synchronized int getHighestNotEmptyPriority() {
        if (isEmpty()) {
            return -1;
        }
        return byPriority.lastKey();
    }

    @Override
    public synchronized boolean contains(@Nonnull final T t) {
        for (NavigableMap.Entry<Integer, ArrayDeque<T>> e : byPriority.entrySet()) {
            if (e.getValue().contains(t)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized boolean containsAll(final @Nonnull Collection<T> iterable) {
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
    public synchronized boolean remove(final @Nonnull T t) {
        for (NavigableMap.Entry<Integer, ArrayDeque<T>> e : byPriority.entrySet()) {
            if (e.getValue().contains(t)) {
                e.getValue().remove(t);
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized boolean removeAll(final @Nonnull Collection<T> iterable) {
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
        boolean result = false;
        for (NavigableMap.Entry<Integer, ArrayDeque<T>> e : byPriority.entrySet()) {
            if (e.getValue().retainAll(iterable)) {
                result = true;
            }
        }
        return result;
    }

    @Override
    public synchronized @Nonnull List<T> toList() {
        List<T> result = new ArrayList<>();
        // Use descendingMap to iterate from the highest priority to the lowest
        for (ArrayDeque<T> bucket : byPriority.descendingMap().values()) {
            if (policy == Policy.FIFO) {
                // FIFO: elements are returned in the order they were added
                result.addAll(bucket);
            } else {
                // LIFO: elements are returned in reverse order of addition
                bucket.descendingIterator().forEachRemaining(result::add);
            }
        }
        return result;
    }

    @Override
    public synchronized @Nonnull Iterator<T> iterator() {
        return new PriorityIterator();
    }

    private class PriorityIterator implements Iterator<T> {
        private final Iterator<Map.Entry<Integer, ArrayDeque<T>>> bucketIterator;
        private Iterator<T> currentDequeIterator;
        private ArrayDeque<T> currentDeque;
        private Integer currentPriority;

        PriorityIterator() {
            // Traverse in descending order to respect priority (highest first)
            this.bucketIterator = byPriority.descendingMap().entrySet().iterator();
        }

        @Override
        public boolean hasNext() {
            while ((currentDequeIterator == null || !currentDequeIterator.hasNext()) && bucketIterator.hasNext()) {
                Map.Entry<Integer, ArrayDeque<T>> entry = bucketIterator.next();
                currentPriority = entry.getKey();
                currentDeque = entry.getValue();

                // Initialize the bucket iterator based on the current policy
                currentDequeIterator = (policy == Policy.FIFO)
                        ? currentDeque.iterator()
                        : currentDeque.descendingIterator();
            }
            return currentDequeIterator != null && currentDequeIterator.hasNext();
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

            synchronized (GeneralPurposePriorityDeque.this) {
                currentDequeIterator.remove();
                if (currentDeque.isEmpty()) {
                    // Use the outer class reference to remove the bucket if empty
                    byPriority.remove(currentPriority);
                    nonEmptyCount--;
                }
            }
        }
    }

}
