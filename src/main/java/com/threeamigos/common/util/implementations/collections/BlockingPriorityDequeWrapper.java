package com.threeamigos.common.util.implementations.collections;

import com.threeamigos.common.util.interfaces.collections.PriorityDeque;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A thread-safe, blocking wrapper for PriorityDeque.
 * This class allows the PriorityDeque to be used as a standard BlockingQueue (e.g., in a ThreadPoolExecutor).
 * Note: standard BlockingQueue methods use the 'defaultPriority' provided in the constructor.
 * Use {@link #add(T, int)} to add tasks with specific priorities.
 */
public class BlockingPriorityDequeWrapper<T> implements BlockingQueue<T> {

    public static final int DEFAULT_PRIORITY = 0;

    private final PriorityDeque<T> delegate;
    private final int defaultPriority;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();

    public BlockingPriorityDequeWrapper(PriorityDeque<T> delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("Delegate cannot be null");
        }
        this.delegate = delegate;
        this.defaultPriority = DEFAULT_PRIORITY;
    }

    public BlockingPriorityDequeWrapper(PriorityDeque<T> delegate, int defaultPriority) {
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
    public void add(T T, int priority) {
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
    public boolean add(@NonNull T T) {
        add(T, defaultPriority);
        return true;
    }

    @Override
    public boolean offer(@NonNull T T) {
        // Our Deques are generally unbounded, so offer is the same as add
        return add(T);
    }

    @Override
    public void put(@NonNull T T) throws InterruptedException {
        // Since the internal deque is unbounded, this never actually blocks
        add(T);
    }

    @Override
    public boolean offer(T T, long timeout, @NonNull TimeUnit unit) {
        return add(T);
    }

    @Override
    public @NonNull T take() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            while (delegate.isEmpty()) {
                notEmpty.await();
            }
            return delegate.poll();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public T poll(long timeout, TimeUnit unit) throws InterruptedException {
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
    public T poll() {
        lock.lock();
        try {
            return delegate.poll();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public T peek() {
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
    public T remove() {
        T r = poll();
        if (r == null) {
            throw new NoSuchElementException();
        }
        return r;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean remove(Object o) {
        lock.lock();
        try {
            return o != null && delegate.remove((T) o);
        } finally {
            lock.unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(Object o) {
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
    public T element() {
        T r = peek();
        if (r == null) {
            throw new NoSuchElementException();
        };
        return r;
    }

    @Override
    public int drainTo(@NonNull Collection<? super T> c) {
        return drainTo(c, Integer.MAX_VALUE);
    }

    @Override
    public int drainTo(@NonNull Collection<? super T> c, int maxElements) {
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

    @Override
    public @NonNull Iterator<T> iterator() {
        lock.lock();
        try {
            // Returning the delegate's iterator.
            // Note: Caller must be careful as this doesn't hold the lock during iteration.
            return delegate.iterator();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Object @NonNull [] toArray() {
        lock.lock();
        try {
            return delegate.toList().toArray();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public <E> E @NonNull [] toArray(E @NonNull [] a) {
        lock.lock();
        try {
            return delegate.toList().toArray(a);
        } finally {
            lock.unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean containsAll(@NonNull Collection<?> c) {
        lock.lock();
        try {
            // We cast to Collection<T> to match the PriorityDeque.containsAll(Iterable<T>) signature
            return delegate.containsAll((Collection<T>) c);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        for (T r : c) {
            add(r);
        }
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean removeAll(@NonNull Collection<?> c) {
        lock.lock();
        try {
            return delegate.removeAll((Collection<T>)c);
        } finally {
            lock.unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean retainAll(@NonNull Collection<?> c) {
        lock.lock();
        try {
            return delegate.retainAll((Collection<T>)c);
        } finally {
            lock.unlock();
        }
    }

}