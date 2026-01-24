package com.threeamigos.common.util.interfaces.collections;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

/**
 * A prioritized Deque. When polling, objects with higher priority are returned first.<br/>
 * The default policy is FIFO.<br/>
 * You can, however, filter objects for a given priority or poll them using a specific policy.
 *
 * @param <T> type of the objects stored in the deque
 *
 * @author Stefano Reksten
 */
public interface PriorityDeque<T> {

    enum Policy {
        FIFO, // First-In-First-Out
        LIFO // Last-In-First-Out
    }

    /**
     * Sets the default policy for polling objects from the deque.
     * The default policy is FIFO.
     *
     * @param policy the policy to set
     */
    void setPolicy(final @Nonnull Policy policy);

    /**
     * Gets the policy for polling objects from the deque.
     *
     * @return the policy
     */
    Policy getPolicy();

    /**
     * Adds an object to the deque with a given priority.
     *
     * @param t object to add
     * @param priority priority of the object
     */
    void add(final @Nonnull T t, final int priority);

    /**
     * Retrieves, but does not remove, the head of the highest non-empty priority bucket using the default policy.
     *
     * @return the head of the highest non-empty priority bucket, or null if the deque is empty
     */
    @Nullable T peek();

    /**
     * Retrieves, but does not remove, the head of the highest non-empty priority bucket using a FIFO policy.
     *
     * @return the oldest object (first added) of the highest non-empty priority bucket, or null if the deque is empty
     */
    @Nullable T peekFifo();

    /**
     * Retrieves, but does not remove, the head of the highest non-empty priority bucket using a LIFO policy.
     *
     * @return the newest object (last added) of the highest non-empty priority bucket, or null if the deque is empty
     */
    @Nullable T peekLifo();

    /**
     * Retrieves and removes the head of the highest non-empty priority bucket using the default policy.
     *
     * @return the head of the highest non-empty priority bucket, or null if the deque is empty
     */
    @Nullable T poll();

    /**
     * Retrieves and removes the head of the highest non-empty priority bucket using a FIFO policy.
     *
     * @return the oldest object (first added) of the highest non-empty priority bucket, or null if the deque is empty
     */
    @Nullable T pollFifo();

    /**
     * Retrieves and removes the head of the highest non-empty priority bucket using a LIFO policy.
     *
     * @return the newest object (last added) of the highest non-empty priority bucket, or null if the deque is empty
     */
    @Nullable T pollLifo();

    /**
     * @return true if no objects are stored, false otherwise
     */
    boolean isEmpty();

    /**
     * @return total number of objects in the deque.
     */
    int size();

    /**
     * Tells if the deque contains the given object at any priority.
     *
     * @param t object to check
     * @return true if the deque contains the given object at any priority, false otherwise.
     */
    boolean contains(final @Nullable T t);

    /**
     * Tells if the deque contains all objects in the given iterable whatever the priority.
     *
     * @param iterable an iterable of objects to check
     * @return true if the deque contains all objects in the iterable whatever the priority, false otherwise.
     */
    boolean containsAll(final @Nonnull Collection<T> iterable);

    /**
     * Clears the deque.
     */
    void clear();

    /**
     * Clears all objects that satisfy the given filtering function.
     *
     * @param filteringFunction filtering function; if true, then the object is removed
     */
    void clear(final @Nonnull Function<T, Boolean>filteringFunction);

    /**
     * Removes an element from the highest non-empty priority bucket.
     * @return true if the deque was modified as a result of the call, false otherwise.
     */
    boolean remove();

    /**
     * Removes the first occurrence of the specified element from this deque if it is present.
     * @param t object to remove
     * @return true if the deque contained the specified element.
     */
    boolean remove(final @Nullable T t);

    /**
     * Removes all objects from the deque whatever the priority.
     * @param iterable an iterable of objects to remove.
     * @return true if this deque changed as a result of the call.
     */
    boolean removeAll(final @Nonnull Collection<T> iterable);

    /**
     * Retains only the elements in this deque that are contained in the given priority bucket.
     * @param iterable an iterable of objects to retain.
     * @return true if this deque changed as a result of the call.
     */
    boolean retainAll(final @Nonnull Collection<T> iterable);

    /**
     * @return a list of all objects in the deque whatever the priority.
     */
    @Nonnull List<T> toList();

    /**
     * @return an iterator over all objects in the deque whatever the priority.
     */
    @Nonnull Iterator<T> iterator();

    /**
     * @return the maximum priority between all objects in the deque.
     */
    int getHighestNotEmptyPriority();

    /**
     * Retrieves, but does not remove, the head of the given priority bucket using the default policy.
     *
     * @param priority priority of the bucket
     * @return the head of the given priority bucket, or null if the bucket is empty
     */
    @Nullable T peek(final int priority);

    /**
     * Retrieves, but does not remove, the head of the given priority bucket using a FIFO policy.
     *
     * @param priority priority of the bucket
     * @return the oldest object (first added) of the given priority bucket, or null if the bucket is empty
     */
    @Nullable T peekFifo(final int priority);

    /**
     * Retrieves, but does not remove, the head of the given priority bucket using a LIFO policy.
     *
     * @param priority priority of the bucket
     * @return the newest object (last added) of the given priority bucket, or null if the bucket is empty
     */
    @Nullable T peekLifo(final int priority);

    /**
     * Retrieves and removes an object from the given priority bucket using the default policy.
     *
     * @param priority priority of the bucket
     * @return the head of the given priority bucket, or null if the bucket is empty
     */
    @Nullable T poll(final int priority);

    /**
     * Retrieves and removes an object from the given priority bucket using a FIFO policy.
     *
     * @param priority priority of the object to retrieve
     * @return the oldest object in the given priority bucket, or null if the bucket is empty
     */
    @Nullable T pollFifo(final int priority);

    /**
     * Retrieves and removes an object from the given priority bucket using a LIFO policy.
     *
     * @param priority priority of the object to retrieve
     * @return the newest object in the given priority bucket, or null if the bucket is empty
     */
    @Nullable T pollLifo(final int priority);

    /**
     * Returns true if no objects are stored in the given priority bucket.
     *
     * @param priority priority of the objects to check
     * @return true if no objects are stored in the given priority bucket, false otherwise
     */
    boolean isEmpty(final int priority);

    /**
     * Returns the number of objects in the given priority bucket.
     *
     * @param priority priority of the objects to count
     * @return the number of objects in the given priority bucket
     */
    int size(final int priority);

    /**
     * Tells if the deque contains the given object at the given priority.
     *
     * @param t object to check
     * @param priority priority of the object to check
     * @return true if the deque contains the given object at the given priority, false otherwise.
     */
    boolean contains(final @Nullable T t, final int priority);

    /**
     * Tells if the deque contains all objects in the given iterable at the given priority.
     *
     * @param iterable an iterable of objects to check
     * @param priority priority of the objects to check
     * @return true if the deque contains all objects in the iterable at the given priority, false otherwise.
     */
    boolean containsAll(final @Nonnull Collection<T> iterable, final int priority);

    /**
     * Clears all objects with given priority.
     *
     * @param priority priority of objects to be removed
     */
    void clear(final int priority);

    /**
     * Clears all objects that satisfy the given filtering function.
     *
     * @param filteringFunction filtering function; if true, then the object is removed
     * @param priority priority of objects to be removed
     */
    void clear(final @Nonnull Function<T, Boolean>filteringFunction, final int priority);

    /**
     * Removes an element from the given priority bucket.
     * @param priority priority of the object to remove
     * @return true if the bucket was modified as a result of the call, false otherwise.
     */
    boolean remove(final int priority);

    /**
     * Removes the first occurrence of the specified element from this deque if it is present for a given priority.
     * @param t object to remove
     * @param priority priority of the object to remove
     * @return true if the deque contained the specified element.
     */
    boolean remove(final @Nullable T t, final int priority);

    /**
     * Removes all objects from the deque in the given priority bucket.
     * @param iterable an iterable of objects to remove.
     * @param priority priority of objects to remove.
     * @return true if this deque changed as a result of the call.
     */
    boolean removeAll(final @Nonnull Collection<T> iterable, final int priority);

    /**
     * Retains only the elements in this deque that are contained in the given priority bucket.
     * @param iterable an iterable of objects to retain.
     * @param priority priority of objects to retain.
     * @return true if this deque changed as a result of the call.
     */
    boolean retainAll(final @Nonnull Collection<T> iterable, final int priority);

    /**
     * @param priority priority of objects
     * @return a list of all objects in the given priority bucket.
     */
    @Nonnull List<T> toList(final int priority);

    /**
     * @param priority priority of objects
     * @return an iterator over all objects in the given priority bucket.
     */
    @Nonnull Iterator<T> iterator(final int priority);
}
