package com.threeamigos.common.util.implementations.collections;

import com.threeamigos.common.util.interfaces.collections.PriorityDeque;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

/**
 * Thread-safe wrapper for PriorityDeque implementations.
 * Uses a read/write lock to synchronize access while keeping business logic in the delegate.
 *
 * @param <T> type of the objects stored in the deque
 *
 * @author Stefano Reksten
 */
public class SynchronizedPriorityDequeWrapper<T> implements PriorityDeque<T> {

    private final PriorityDeque<T> delegate;
    private final ReentrantReadWriteLock rw = new ReentrantReadWriteLock();
    private final Lock read = rw.readLock();
    private final Lock write = rw.writeLock();

    public SynchronizedPriorityDequeWrapper(final @Nonnull PriorityDeque<T> delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("Delegate cannot be null");
        }
        this.delegate = delegate;
    }

    @Override
    public void setPolicy(@Nonnull Policy policy) {
        write.lock();
        try {
            delegate.setPolicy(policy);
        } finally {
            write.unlock();
        }
    }

    @Override
    public Policy getPolicy() {
        read.lock();
        try {
            return delegate.getPolicy();
        } finally {
            read.unlock();
        }
    }

    @Override
    public void add(@Nonnull T t, int priority) {
        write.lock();
        try {
            delegate.add(t, priority);
        } finally {
            write.unlock();
        }
    }

    @Override
    public @Nullable T peek() {
        read.lock();
        try {
            return delegate.peek();
        } finally {
            read.unlock();
        }
    }

    @Override
    public @Nullable T peekFifo() {
        read.lock();
        try {
            return delegate.peekFifo();
        } finally {
            read.unlock();
        }
    }

    @Override
    public @Nullable T peekLifo() {
        read.lock();
        try {
            return delegate.peekLifo();
        } finally {
            read.unlock();
        }
    }

    @Override
    public @Nullable T poll() {
        write.lock();
        try {
            return delegate.poll();
        } finally {
            write.unlock();
        }
    }

    @Override
    public @Nullable T pollFifo() {
        write.lock();
        try {
            return delegate.pollFifo();
        } finally {
            write.unlock();
        }
    }

    @Override
    public @Nullable T pollLifo() {
        write.lock();
        try {
            return delegate.pollLifo();
        } finally {
            write.unlock();
        }
    }

    @Override
    public boolean isEmpty() {
        read.lock();
        try {
            return delegate.isEmpty();
        } finally {
            read.unlock();
        }
    }

    @Override
    public int size() {
        read.lock();
        try {
            return delegate.size();
        } finally {
            read.unlock();
        }
    }

    @Override
    public boolean contains(@Nonnull T t) {
        read.lock();
        try {
            return delegate.contains(t);
        } finally {
            read.unlock();
        }
    }

    @Override
    public boolean containsAll(@Nonnull Collection<T> iterable) {
        read.lock();
        try {
            return delegate.containsAll(iterable);
        } finally {
            read.unlock();
        }
    }

    @Override
    public void clear() {
        write.lock();
        try {
            delegate.clear();
        } finally {
            write.unlock();
        }
    }

    @Override
    public void clear(@Nonnull Function<T, Boolean> filteringFunction) {
        write.lock();
        try {
            delegate.clear(filteringFunction);
        } finally {
            write.unlock();
        }
    }

    @Override
    public boolean remove() {
        write.lock();
        try {
            return delegate.remove();
        } finally {
            write.unlock();
        }
    }

    @Override
    public boolean remove(@Nonnull T t) {
        write.lock();
        try {
            return delegate.remove(t);
        } finally {
            write.unlock();
        }
    }

    @Override
    public boolean removeAll(@Nonnull Collection<T> iterable) {
        write.lock();
        try {
            return delegate.removeAll(iterable);
        } finally {
            write.unlock();
        }
    }

    @Override
    public boolean retainAll(@Nonnull Collection<T> iterable) {
        write.lock();
        try {
            return delegate.retainAll(iterable);
        } finally {
            write.unlock();
        }
    }

    @Override
    public @Nonnull List<T> toList() {
        read.lock();
        try {
            return delegate.toList();
        } finally {
            read.unlock();
        }
    }

    /**
     * Returns an iterator over all elements in the deque.
     * <p><b>Implementation Note - Snapshot Iterator:</b>
     * The returned iterator is a <i>snapshot</i> taken at the time of this call.
     * It will not reflect subsequent modifications to the deque (additions, removals, or reordering).
     * This ensures thread-safety without holding locks during iteration.
     * <p><b>Pros:</b>
     * <ul>
     * <li>Thread-safe: No synchronization needed during iteration</li>
     * <li>No deadlock risk: Lock is released before iteration begins</li>
     * <li>Predictable: Iterator sees consistent point-in-time state</li>
     * <li>Allows concurrent operations: Other threads can modify deque during iteration</li>
     * <li>Uses read lock: Multiple threads can create snapshots concurrently</li>
     * </ul>
     * <p><b>Cons:</b>
     * <ul>
     * <li>Memory overhead: Creates a copy of all elements</li>
     * <li>Not a live view: Subsequent changes not visible to iterator</li>
     * <li>Performance: O(n) time and space to create snapshot</li>
     * </ul>
     * <p>This is the standard pattern used by concurrent collections like
     * {@link java.util.concurrent.CopyOnWriteArrayList}.
     *
     * @return a thread-safe snapshot iterator over all elements
     */
    @Override
    public @Nonnull Iterator<T> iterator() {
        read.lock();
        try {
            return delegate.toList().iterator();
        } finally {
            read.unlock();
        }
    }

    @Override
    public int getHighestNotEmptyPriority() {
        read.lock();
        try {
            return delegate.getHighestNotEmptyPriority();
        } finally {
            read.unlock();
        }
    }

    @Override
    public @Nullable T peek(int priority) {
        read.lock();
        try {
            return delegate.peek(priority);
        } finally {
            read.unlock();
        }
    }

    @Override
    public @Nullable T peekFifo(int priority) {
        read.lock();
        try {
            return delegate.peekFifo(priority);
        } finally {
            read.unlock();
        }
    }

    @Override
    public @Nullable T peekLifo(int priority) {
        read.lock();
        try {
            return delegate.peekLifo(priority);
        } finally {
            read.unlock();
        }
    }

    @Override
    public @Nullable T poll(int priority) {
        write.lock();
        try {
            return delegate.poll(priority);
        } finally {
            write.unlock();
        }
    }

    @Override
    public @Nullable T pollFifo(int priority) {
        write.lock();
        try {
            return delegate.pollFifo(priority);
        } finally {
            write.unlock();
        }
    }

    @Override
    public @Nullable T pollLifo(int priority) {
        write.lock();
        try {
            return delegate.pollLifo(priority);
        } finally {
            write.unlock();
        }
    }

    @Override
    public boolean isEmpty(int priority) {
        read.lock();
        try {
            return delegate.isEmpty(priority);
        } finally {
            read.unlock();
        }
    }

    @Override
    public int size(int priority) {
        read.lock();
        try {
            return delegate.size(priority);
        } finally {
            read.unlock();
        }
    }

    @Override
    public boolean contains(@Nonnull T t, int priority) {
        read.lock();
        try {
            return delegate.contains(t, priority);
        } finally {
            read.unlock();
        }
    }

    @Override
    public boolean containsAll(@Nonnull Collection<T> iterable, int priority) {
        read.lock();
        try {
            return delegate.containsAll(iterable, priority);
        } finally {
            read.unlock();
        }
    }

    @Override
    public void clear(int priority) {
        write.lock();
        try {
            delegate.clear(priority);
        } finally {
            write.unlock();
        }
    }

    @Override
    public void clear(@Nonnull Function<T, Boolean> filteringFunction, int priority) {
        write.lock();
        try {
            delegate.clear(filteringFunction, priority);
        } finally {
            write.unlock();
        }
    }

    @Override
    public boolean remove(int priority) {
        write.lock();
        try {
            return delegate.remove(priority);
        } finally {
            write.unlock();
        }
    }

    @Override
    public boolean remove(@Nonnull T t, int priority) {
        write.lock();
        try {
            return delegate.remove(t, priority);
        } finally {
            write.unlock();
        }
    }

    @Override
    public boolean removeAll(@Nonnull Collection<T> iterable, int priority) {
        write.lock();
        try {
            return delegate.removeAll(iterable, priority);
        } finally {
            write.unlock();
        }
    }

    @Override
    public boolean retainAll(@Nonnull Collection<T> iterable, int priority) {
        write.lock();
        try {
            return delegate.retainAll(iterable, priority);
        } finally {
            write.unlock();
        }
    }

    @Override
    public @Nonnull List<T> toList(int priority) {
        read.lock();
        try {
            return delegate.toList(priority);
        } finally {
            read.unlock();
        }
    }

    /**
     * Returns an iterator over elements in the specified priority bucket.
     * <p><b>Implementation Note - Snapshot Iterator:</b>
     * The returned iterator is a <i>snapshot</i> taken at the time of this call.
     * It will not reflect later modifications to the priority bucket.
     * This ensures thread-safety without holding locks during iteration.
     * <p>See {@link #iterator()} for detailed pros and cons of the snapshot approach.
     *
     * @param priority the priority bucket to iterate over
     * @return a thread-safe snapshot iterator over elements at the specified priority
     */
    @Override
    public @Nonnull Iterator<T> iterator(int priority) {
        read.lock();
        try {
            return delegate.toList(priority).iterator();
        } finally {
            read.unlock();
        }
    }
}
