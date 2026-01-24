package com.threeamigos.common.util.implementations.collections;

import com.threeamigos.common.util.interfaces.collections.PriorityDeque;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;
import java.util.function.Function;

/**
 * General-purpose implementation of a {@link PriorityDeque} (arbitrary priority integers).
 * Works for any integer priorities, sparse ranges, dynamic inserts/removals.<br/>
 * <b>NOTE:</b> This implementation is not thread-safe. For that, wrap this class with
 * {@link SynchronizedPriorityDequeWrapper}.<br/>
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
    private int elementCount = 0;

    public GeneralPurposePriorityDeque() {
        this.policy = Policy.FIFO;
    }

    public GeneralPurposePriorityDeque(final Policy policy) {
        validatePolicy(policy);
        this.policy = policy;
    }

    public void setPolicy(@Nonnull final Policy policy) {
        validatePolicy(policy);
        this.policy = policy;
    }

    public Policy getPolicy() {
        return policy;
    }

    public void add(@Nonnull final T t, final int priority) {
        validateObject(t);
        ArrayDeque<T> q = byPriority.computeIfAbsent(priority, p -> {
            nonEmptyCount++;
            return new ArrayDeque<>();
        });
        q.addLast(t);
        elementCount++;
    }

    @Override
    public void addAll(@Nonnull Collection<T> iterable, int priority) {
        validateCollection(iterable);
        for (T t : iterable) {
            add(t, priority);
        }
    }

    public @Nullable T peek() {
        return policy == Policy.FIFO ? peekFifo() : peekLifo();
    }

    public @Nullable T peekFifo() {
        Map.Entry<Integer, ArrayDeque<T>> integerArrayDequeEntry = byPriority.lastEntry();
        return integerArrayDequeEntry != null ? integerArrayDequeEntry.getValue().peekFirst() : null;
    }

    public @Nullable T peekLifo() {
        Map.Entry<Integer, ArrayDeque<T>> integerArrayDequeEntry = byPriority.lastEntry();
        return integerArrayDequeEntry != null ? integerArrayDequeEntry.getValue().peekLast() : null;
    }

    @Override
    public @Nullable T peek(final int priority) {
        return policy == Policy.FIFO ? peekFifo(priority) : peekLifo(priority);
    }

    @Override
    public @Nullable T peekFifo(final int priority) {
        ArrayDeque<T> q = byPriority.get(priority);
        return q != null ? q.peekFirst() : null;
    }

    @Override
    public @Nullable T peekLifo(final int priority) {
        ArrayDeque<T> q = byPriority.get(priority);
        return q != null ? q.peekLast() : null;
    }

    public @Nullable T poll() {
        if (policy == Policy.FIFO) {
            return pollFifo();
        } else {
            return pollLifo();
        }
    }

    @Override
    public @Nullable T poll(final int priority) {
        return policy == Policy.FIFO ? pollFifo(priority) : pollLifo(priority);
    }

    /** Take the next task preferring the highest priority, FIFO within that priority */
    public @Nullable T pollFifo() {
        Map.Entry<Integer, ArrayDeque<T>> e = byPriority.lastEntry();
        if (e == null) {
            return null;
        }
        ArrayDeque<T> q = e.getValue();
        T t = q.pollFirst();
        if (t != null) {
            elementCount--;
        }
        if (q.isEmpty()) {
            byPriority.remove(e.getKey());
            nonEmptyCount--;
        }
        return t;
    }

    public @Nullable T pollFifo(final int priority) {
        ArrayDeque<T> q = byPriority.get(priority);
        if (q == null) {
            return null;
        }
        T t = q.pollFirst();
        if (t != null) {
            elementCount--;
        }
        if (q.isEmpty()) {
            byPriority.remove(priority);
            nonEmptyCount--;
        }
        return t;
    }

    /** Take the next task preferring the highest priority, LIFO within that priority */
    public @Nullable T pollLifo() {
        Map.Entry<Integer, ArrayDeque<T>> e = byPriority.lastEntry();
        if (e == null) {
            return null;
        }
        ArrayDeque<T> q = e.getValue();
        T t = q.pollLast();
        if (t != null) {
            elementCount--;
        }
        if (q.isEmpty()) {
            byPriority.remove(e.getKey());
            nonEmptyCount--;
        }
        return t;
    }

    public @Nullable T pollLifo(final int priority) {
        ArrayDeque<T> q = byPriority.get(priority);
        if (q == null) {
            return null;
        }
        T t = q.pollLast();
        if (t != null) {
            elementCount--;
        }
        if (q.isEmpty()) {
            byPriority.remove(priority);
            nonEmptyCount--;
        }
        return t;
    }

    public boolean isEmpty() {
        return nonEmptyCount == 0;
    }

    public boolean isEmpty(final int priority) {
        ArrayDeque<T> q = byPriority.get(priority);
        return q == null || q.isEmpty();
    }

    public int size() {
        return elementCount;
    }

    public int size(final int priority) {
        ArrayDeque<T> q = byPriority.get(priority);
        return q == null ? 0 : q.size();
    }

    public void clear() {
        byPriority.clear();
        nonEmptyCount = 0;
        elementCount = 0;
    }

    public void clear(final int priority) {
        ArrayDeque<T> q = byPriority.remove(priority);
        if (q != null) {
            elementCount -= q.size();
            nonEmptyCount--;
        }
    }

    public void clear(@Nonnull final Function<T, Boolean> filteringFunction) {
        validateFilteringFunction(filteringFunction);
        List<Integer> emptyPriorities = new ArrayList<>();
        for (Map.Entry<Integer, ArrayDeque<T>> entry : byPriority.entrySet()) {
            ArrayDeque<T> q = entry.getValue();
            int beforeSize = q.size();
            q.removeIf(filteringFunction::apply);
            elementCount -= (beforeSize - q.size());
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
    public void clear(@Nonnull final Function<T, Boolean> filteringFunction, final int priority) {
        validateFilteringFunction(filteringFunction);
        ArrayDeque<T> q = byPriority.get(priority);
        if (q != null) {
            int beforeSize = q.size();
            q.removeIf(filteringFunction::apply);
            elementCount -= (beforeSize - q.size());
            if (q.isEmpty()) {
                byPriority.remove(priority);
                nonEmptyCount--;
            }
        }
    }

    public int getHighestNotEmptyPriority() {
        if (isEmpty()) {
            return -1;
        }
        return byPriority.lastKey();
    }

    @Override
    public boolean contains(@Nullable final T t) {
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
    public boolean contains(@Nullable final T t, final int priority) {
        if (t == null) {
            return false;
        }
        ArrayDeque<T> q = byPriority.get(priority);
        return q != null && q.contains(t);
    }

    @Override
    public boolean containsAll(final @Nonnull Collection<T> iterable) {
        validateCollection(iterable);
        for (T t : iterable) {
            if (t == null || !contains(t)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(final @Nonnull Collection<T> iterable, final int priority) {
        validateCollection(iterable);
        ArrayDeque<T> q = byPriority.get(priority);
        if (q == null) {
            return iterable.isEmpty();
        }
        return q.containsAll(iterable);
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
    public boolean remove(final @Nullable T t) {
        if (t == null) {
            return false;
        }
        for (Map.Entry<Integer, ArrayDeque<T>> e : byPriority.entrySet()) {
            if (e.getValue().remove(t)) {
                elementCount--;
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
    public boolean remove(final int priority) {
        T t = poll(priority);
        if (t == null) {
            throw new NoSuchElementException();
        }
        return true;
    }

    @Override
    public boolean remove(final @Nullable T t, final int priority) {
        if (t == null) {
            return false;
        }
        ArrayDeque<T> q = byPriority.get(priority);
        if (q != null && q.remove(t)) {
            elementCount--;
            if (q.isEmpty()) {
                byPriority.remove(priority);
                nonEmptyCount--;
            }
            return true;
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
    public boolean removeAll(final @Nonnull Collection<T> iterable, final int priority) {
        validateCollection(iterable);
        ArrayDeque<T> q = byPriority.get(priority);
        if (q == null) {
            return false;
        }
        int beforeSize = q.size();
        boolean removed = q.removeAll(iterable);
        if (removed) {
            elementCount -= (beforeSize - q.size());
        }
        if (removed && q.isEmpty()) {
            byPriority.remove(priority);
            nonEmptyCount--;
        }
        return removed;
    }

    @Override
    public boolean retainAll(final @Nonnull Collection<T> iterable) {
        validateCollection(iterable);
        boolean result = false;
        List<Integer> emptyPriorities = new ArrayList<>();
        for (Map.Entry<Integer, ArrayDeque<T>> e : byPriority.entrySet()) {
            ArrayDeque<T> q = e.getValue();
            int beforeSize = q.size();
            if (q.retainAll(iterable)) {
                elementCount -= (beforeSize - q.size());
                result = true;
                if (q.isEmpty()) {
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
    public boolean retainAll(final @Nonnull Collection<T> iterable, final int priority) {
        validateCollection(iterable);
        ArrayDeque<T> q = byPriority.get(priority);
        if (q == null) {
            return false;
        }
        int beforeSize = q.size();
        boolean changed = q.retainAll(iterable);
        if (changed) {
            elementCount -= (beforeSize - q.size());
        }
        if (changed && q.isEmpty()) {
            byPriority.remove(priority);
            nonEmptyCount--;
        }
        return changed;
    }

    @Override
    public @Nonnull List<T> toList() {
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
    public @Nonnull List<T> toList(final int priority) {
        ArrayDeque<T> q = byPriority.get(priority);
        return q != null ? new ArrayList<>(q) : new ArrayList<>();
    }

    @Override
    public @Nonnull Iterator<T> iterator() {
        return new PriorityIterator();
    }

    @Override
    public @Nonnull Iterator<T> iterator(final int priority) {
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
            currentDequeIterator.remove();
            elementCount--;
            if (currentDeque.isEmpty()) {
                if (singlePriority) {
                    byPriority.remove(currentPriority);
                } else {
                    bucketIterator.remove();
                }
                nonEmptyCount--;
            }
        }
    }

    private void validatePolicy(@Nonnull Policy policy) {
        if (policy == null) {
            throw new IllegalArgumentException("Policy cannot be null");
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
