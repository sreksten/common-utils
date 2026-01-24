package com.threeamigos.common.util.implementations.collections;

import com.threeamigos.common.util.interfaces.collections.PriorityDeque;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A thread-safe, blocking wrapper for PriorityDeque.<br/>
 * This class allows the PriorityDeque to be used as a standard BlockingQueue (e.g., in a ThreadPoolExecutor).<br/>
 * <i>Note:</i> standard BlockingQueue methods use the 'defaultPriority' provided in the constructor.<br/>
 * Use {@link #add(T, int)} to add tasks with specific priorities.
 * 
 * @author Stefano Reksten
 */
public class BlockingPriorityDequeWrapper<T> implements BlockingQueue<T> {

    public static final int DEFAULT_PRIORITY = 0;

    private final PriorityDeque<T> delegate;
    private final int defaultPriority;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();

    public BlockingPriorityDequeWrapper(final @Nonnull PriorityDeque<T> delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("Delegate cannot be null");
        }
        this.delegate = delegate;
        this.defaultPriority = DEFAULT_PRIORITY;
    }

    public BlockingPriorityDequeWrapper(final @Nonnull PriorityDeque<T> delegate, final int defaultPriority) {
        if (delegate == null) {
            throw new IllegalArgumentException("Delegate cannot be null");
        }
        this.delegate = delegate;
        this.defaultPriority = defaultPriority;
    }

    public int getDefaultPriority() {
        return defaultPriority;
    }

    /**
     * Non-standard method to add a task with a specific priority.
     * Signals any waiting threads that the queue is no longer empty.
     */
    public void add(final @Nonnull T T, final int priority) {
        if (T == null) {
            throw new IllegalArgumentException("Element to add cannot be null");
        }
        lock.lock();
        try {
            delegate.add(T, priority);
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    // --- BlockingQueue / Queue Implementation ---

    @Override
    public boolean add(final @Nonnull T T) {
        add(T, defaultPriority);
        return true;
    }

    @Override
    public boolean offer(final @Nonnull T T) {
        // Our deques are generally unbounded, so offer is the same as add
        return add(T);
    }

    @Override
    public void put(final @Nonnull T T) throws InterruptedException {
        // Since the internal deque is unbounded, this never actually blocks
        add(T);
    }

    @Override
    public boolean offer(final T T, final long timeout, final @Nonnull TimeUnit unit) {
        validateTimeUnit(unit);
        return add(T);
    }

    @Override
    public @Nonnull T take() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            while (delegate.isEmpty()) {
                notEmpty.await();
            }
            return Objects.requireNonNull(delegate.poll());
        } finally {
            lock.unlock();
        }
    }

    @Override
    public @Nullable T poll(final long timeout, final @Nonnull TimeUnit unit) throws InterruptedException {
        validateTimeUnit(unit);
        long nanos = unit.toNanos(timeout);
        lock.lockInterruptibly();
        try {
            while (delegate.isEmpty()) {
                if (nanos <= 0) return null;
                nanos = notEmpty.awaitNanos(nanos);
            }
            return delegate.poll();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public @Nullable T poll() {
        lock.lock();
        try {
            return delegate.poll();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public @Nullable T peek() {
        lock.lock();
        try {
            return delegate.peek();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int remainingCapacity() {
        return Integer.MAX_VALUE; // Unbounded
    }

    @Override
    public @Nonnull T remove() {
        T r = poll();
        if (r == null) {
            throw new NoSuchElementException();
        }
        return r;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean remove(final Object o) {
        lock.lock();
        try {
            return o != null && delegate.remove((T) o);
        } finally {
            lock.unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(final Object o) {
        lock.lock();
        try {
            // PriorityDeque is typed, so we check if the object is a T
            return o != null && delegate.contains((T) o);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int size() {
        lock.lock();
        try {
            return delegate.size();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isEmpty() {
        lock.lock();
        try {
            return delegate.isEmpty();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void clear() {
        lock.lock();
        try {
            delegate.clear();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public @Nonnull T element() {
        T r = peek();
        if (r == null) {
            throw new NoSuchElementException();
        }
        return r;
    }

    @Override
    public int drainTo(final @Nonnull Collection<? super T> c) {
        return drainTo(c, Integer.MAX_VALUE);
    }

    @Override
    public int drainTo(final @Nonnull Collection<? super T> c, int maxElements) {
        validateCollection(c);
        lock.lock();
        try {
            int n = 0;
            while (n < maxElements && !delegate.isEmpty()) {
                c.add(delegate.poll());
                n++;
            }
            return n;
        } finally {
            lock.unlock();
        }
    }

    /**
     * <i>Note</i>: Caller must be careful as this doesn't hold the lock during iteration.
     * 
     * @return the delegate's iterator
     */
    @Override
    public @Nonnull Iterator<T> iterator() {
        lock.lock();
        try {
            return delegate.iterator();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public @Nonnull Object[] toArray() {
        lock.lock();
        try {
            return delegate.toList().toArray();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public @Nonnull <E> E[] toArray(@Nonnull E[] a) {
        lock.lock();
        try {
            return delegate.toList().toArray(a);
        } finally {
            lock.unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean containsAll(@Nonnull Collection<?> c) {
        validateCollection(c);
        lock.lock();
        try {
            // We cast to Collection<T> to match the PriorityDeque.containsAll(Iterable<T>) signature
            return delegate.containsAll((Collection<T>) c);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean addAll(@Nonnull Collection<? extends T> c) {
        validateCollection(c);
        for (T r : c) {
            add(r);
        }
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean removeAll(@Nonnull Collection<?> c) {
        validateCollection(c);
        lock.lock();
        try {
            return delegate.removeAll((Collection<T>)c);
        } finally {
            lock.unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean retainAll(@Nonnull Collection<?> c) {
        validateCollection(c);
        lock.lock();
        try {
            return delegate.retainAll((Collection<T>)c);
        } finally {
            lock.unlock();
        }
    }

    private void validateCollection(Collection<?> c) {
        if (c == null) {
            throw new IllegalArgumentException("Collection cannot be null");      
        }
    }

    private void validateTimeUnit(@Nonnull TimeUnit unit) {
        if (unit == null) {
            throw new NullPointerException("TimeUnit cannot be null");
        }
    }
}