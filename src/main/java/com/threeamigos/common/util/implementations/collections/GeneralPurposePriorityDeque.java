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

    @Override
    public synchronized T peek(final int priority) {
        return policy == Policy.FIFO ? peekFifo(priority) : peekLifo(priority);
    }

    @Override
    public synchronized T peekFifo(final int priority) {
        ArrayDeque<T> q = byPriority.get(priority);
        return q != null ? q.peekFirst() : null;
    }

    @Override
    public synchronized T peekLifo(final int priority) {
        ArrayDeque<T> q = byPriority.get(priority);
        return q != null ? q.peekLast() : null;
    }

    public synchronized T poll() {
        if (policy == Policy.FIFO) {
            return pollFifo();
        } else {
            return pollLifo();
        }
    }

    @Override
    public synchronized T poll(final int priority) {
        return policy == Policy.FIFO ? pollFifo(priority) : pollLifo(priority);
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
        ArrayDeque<T> q = byPriority.remove(priority);
        if (q != null && !q.isEmpty()) {
            nonEmptyCount--;
        }
    }

    public synchronized void clear(@Nonnull final Function<T, Boolean> filteringFunction) {
        if (filteringFunction == null) {
            throw new IllegalArgumentException("Filtering function cannot be null");
        }
        List<Integer> emptyPriorities = new ArrayList<>();
        for (Map.Entry<Integer, ArrayDeque<T>> entry : byPriority.entrySet()) {
            entry.getValue().removeIf(filteringFunction::apply);
            if (entry.getValue().isEmpty()) {
                emptyPriorities.add(entry.getKey());
            }
        }
        for (Integer priority : emptyPriorities) {
            byPriority.remove(priority);
            nonEmptyCount--;
        }
    }

    @Override
    public synchronized void clear(@Nonnull final Function<T, Boolean> filteringFunction, final int priority) {
        if (filteringFunction == null) {
            throw new IllegalArgumentException("Filtering function cannot be null");
        }
        ArrayDeque<T> q = byPriority.get(priority);
        if (q != null) {
            q.removeIf(filteringFunction::apply);
            if (q.isEmpty()) {
                byPriority.remove(priority);
                nonEmptyCount--;
            }
        }
    }

    public synchronized int getHighestNotEmptyPriority() {
        if (isEmpty()) {
            return -1;
        }
        return byPriority.lastKey();
    }

    @Override
    public synchronized boolean contains(@Nonnull final T t) {
        if (t == null) {
            return false;
        }
        for (NavigableMap.Entry<Integer, ArrayDeque<T>> e : byPriority.entrySet()) {
            if (e.getValue().contains(t)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized boolean contains(@Nonnull final T t, final int priority) {
        if (t == null) {
            return false;
        }
        ArrayDeque<T> q = byPriority.get(priority);
        return q != null && q.contains(t);
    }

    @Override
    public synchronized boolean containsAll(final @Nonnull Collection<T> iterable) {
        if (iterable == null) {
            throw new NullPointerException("Collection cannot be null");
        }
        for (T t : iterable) {
            if (t == null || !contains(t)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public synchronized boolean containsAll(final @Nonnull Collection<T> iterable, final int priority) {
        if (iterable == null) {
            throw new NullPointerException("Collection cannot be null");
        }
        ArrayDeque<T> q = byPriority.get(priority);
        if (q == null) {
            return iterable.isEmpty();
        }
        return q.containsAll(iterable);
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
        if (t == null) {
            return false;
        }
        for (Map.Entry<Integer, ArrayDeque<T>> e : byPriority.entrySet()) {
            if (e.getValue().remove(t)) {
                if (e.getValue().isEmpty()) {
                    byPriority.remove(e.getKey());
                    nonEmptyCount--;
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized boolean remove(final int priority) {
        T t = poll(priority);
        if (t == null) {
            throw new NoSuchElementException();
        }
        return true;
    }

    @Override
    public synchronized boolean remove(final @Nonnull T t, final int priority) {
        if (t == null) {
            return false;
        }
        ArrayDeque<T> q = byPriority.get(priority);
        if (q != null && q.remove(t)) {
            if (q.isEmpty()) {
                byPriority.remove(priority);
                nonEmptyCount--;
            }
            return true;
        }
        return false;
    }

    @Override
    public synchronized boolean removeAll(final @Nonnull Collection<T> iterable) {
        if (iterable == null) {
            throw new NullPointerException("Collection cannot be null");
        }
        boolean result = true;
        for (T t : iterable) {
            if (!remove(t)) {
                result = false;
            }
        }
        return result;
    }

    @Override
    public synchronized boolean removeAll(final @Nonnull Collection<T> iterable, final int priority) {
        if (iterable == null) {
            throw new NullPointerException("Collection cannot be null");
        }
        ArrayDeque<T> q = byPriority.get(priority);
        if (q == null) {
            return false;
        }
        boolean removed = q.removeAll(iterable);
        if (removed && q.isEmpty()) {
            byPriority.remove(priority);
            nonEmptyCount--;
        }
        return removed;
    }

    @Override
    public synchronized boolean retainAll(final @Nonnull Collection<T> iterable) {
        if (iterable == null) {
            throw new NullPointerException("Collection cannot be null");
        }
        boolean result = false;
        List<Integer> emptyPriorities = new ArrayList<>();
        for (Map.Entry<Integer, ArrayDeque<T>> e : byPriority.entrySet()) {
            if (e.getValue().retainAll(iterable)) {
                result = true;
                if (e.getValue().isEmpty()) {
                    emptyPriorities.add(e.getKey());
                }
            }
        }
        for (Integer priority : emptyPriorities) {
            byPriority.remove(priority);
            nonEmptyCount--;
        }
        return result;
    }

    @Override
    public synchronized boolean retainAll(final @Nonnull Collection<T> iterable, final int priority) {
        if (iterable == null) {
            throw new NullPointerException("Collection cannot be null");
        }
        ArrayDeque<T> q = byPriority.get(priority);
        if (q == null) {
            return false;
        }
        boolean changed = q.retainAll(iterable);
        if (changed && q.isEmpty()) {
            byPriority.remove(priority);
            nonEmptyCount--;
        }
        return changed;
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
    public synchronized @Nonnull List<T> toList(final int priority) {
        ArrayDeque<T> q = byPriority.get(priority);
        return q != null ? new ArrayList<>(q) : new ArrayList<>();
    }

    @Override
    public synchronized @Nonnull Iterator<T> iterator() {
        return new PriorityIterator();
    }

    @Override
    public synchronized @Nonnull Iterator<T> iterator(final int priority) {
        return new PriorityIterator(priority);
    }

    private class PriorityIterator implements Iterator<T> {
        private final Iterator<Map.Entry<Integer, ArrayDeque<T>>> bucketIterator;
        private Iterator<T> currentDequeIterator;
        private ArrayDeque<T> currentDeque;
        private Integer currentPriority;
        private final boolean singlePriority;

        PriorityIterator() {
            // Traverse in descending order to respect priority (highest first)
            this.bucketIterator = byPriority.descendingMap().entrySet().iterator();
            this.singlePriority = false;
        }

        PriorityIterator(int priority) {
            // Iterate over a single priority bucket
            ArrayDeque<T> q = byPriority.get(priority);
            if (q != null) {
                Map<Integer, ArrayDeque<T>> singleMap = Collections.singletonMap(priority, q);
                this.bucketIterator = singleMap.entrySet().iterator();
            } else {
                this.bucketIterator = Collections.emptyIterator();
            }
            this.singlePriority = true;
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
