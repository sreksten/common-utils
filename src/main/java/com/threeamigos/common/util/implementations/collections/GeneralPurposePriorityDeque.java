package com.threeamigos.common.util.implementations.collections;

import com.threeamigos.common.util.interfaces.collections.PriorityDeque;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
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
 */
public class GeneralPurposePriorityDeque<T> implements PriorityDeque<T> {

    private final NavigableMap<Integer, ArrayDeque<T>> byPriority = new TreeMap<>();
    private int nonEmptyCount = 0;

    public synchronized int getHighestNotEmptyPriority() {
        if (isEmpty()) {
            return -1;
        }
        return byPriority.lastKey();
    }

    public synchronized void add(T task, int priority) {
        ArrayDeque<T> q = byPriority.computeIfAbsent(priority, p -> {
            nonEmptyCount++;
            return new ArrayDeque<>();
        });
        q.addLast(task);
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

    public synchronized T pollFifo(int priority) {
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

    public synchronized T pollLifo(int priority) {
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

    public synchronized int size() {
        return byPriority.values().stream().mapToInt(ArrayDeque::size).sum();
    }

    public synchronized int size(int priority) {
        ArrayDeque<T> q = byPriority.get(priority);
        return q == null ? 0 : q.size();
    }

    public synchronized void clear() {
        byPriority.clear();
        nonEmptyCount = 0;
    }

    public synchronized void clear(int priority) {
        byPriority.remove(priority);
    }

    public synchronized void clear(Function<T, Boolean> filteringFunction) {
        byPriority.values().forEach(q -> q.removeIf(filteringFunction::apply));
        nonEmptyCount = byPriority.values().stream().mapToInt(ArrayDeque::size).sum();
    }

}
