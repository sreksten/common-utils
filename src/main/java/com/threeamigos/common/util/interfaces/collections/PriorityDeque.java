package com.threeamigos.common.util.interfaces.collections;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

/**
 * A prioritized Deque. When polling, objects with higher priority are returned first.<br/>
 * You can, however, filter objects with a given priority or poll them using FIFO or LIFO policies.
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
     * Sets the policy for polling objects from the deque.
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
     * Adds an object to the deque.
     *
     * @param t object to add
     * @param priority priority of the object
     */
    void add(final @Nonnull T t, final int priority);

    /**
     * Retrieves, but does not remove, the head of this deque (with the highest priority).
     *
     * @return the head of this deque, or null if empty
     */
    @Nullable T peek();

    /**
     * Retrieves, but does not remove, the head of the highest priority bucket (FIFO).
     *
     * @return the head of the highest priority bucket, or null if empty
     */
    @Nullable T peekFifo();

    /**
     * Retrieves, but does not remove, the head of the highest priority bucket (LIFO).
     *
     * @return the head of the highest priority bucket, or null if empty
     */
    @Nullable T peekLifo();

    /**
     * Retrieves and removes an object with the highest priority from the deque.
     * The default policy is FIFO.
     *
     * @return the oldest object with the highest priority, or null if empty
     */
    @Nullable T poll();

    /**
     * Retrieves and removes an object with the highest priority from the deque using a FIFO policy.
     *
     * @return the oldest object with the highest priority, or null if empty
     */
    @Nullable T pollFifo();

    /**
     * Retrieves and removes an object with given priority from the deque using a FIFO policy.
     *
     * @param priority priority of the object to retrieve
     * @return the oldest object with the given priority, or null if empty
     */
    @Nullable T pollFifo(final int priority);

    /**
     * Retrieves and removes an object with the highest priority from the deque using a LIFO policy.
     *
     * @return the newest object with the highest priority, or null if empty
     */
    @Nullable T pollLifo();

    /**
     * Retrieves and removes an object with given priority from the deque using a LIFO policy.
     *
     * @param priority priority of the object to retrieve
     * @return the newest object with a given priority, or null if empty
     */
    @Nullable T pollLifo(final int priority);

    /**
     * @return true if no objects are stored, false otherwise
     */
    boolean isEmpty();

    /**
     * @return true if no objects are stored, false otherwise
     */
    boolean isEmpty(final int priority);

    /**
     * @return total number of objects in the deque.
     */
    int size();

    /**
     * @param priority priority of the objects to count
     * @return the number of objects with given priority in the deque.
     */
    int size(int priority);

    /**
     * Clears the deque.
     */
    void clear();

    /**
     * Clears all objects with given priority.
     * @param priority priority of objects to be removed
     */
    void clear(int priority);

    /**
     * Clears all objects that satisfy the given filtering function.
     *
     * @param filteringFunction filtering function; if true, then the object is removed
     */
    void clear(@Nonnull Function<T, Boolean>filteringFunction);

    /**
     * @return the maximum priority between all objects in the deque.
     */
    int getHighestNotEmptyPriority();

    boolean contains(@Nonnull T t);

    boolean containsAll(@Nonnull Collection<T> iterable);

    boolean remove();

    boolean remove(@Nonnull T t);

    boolean removeAll(@Nonnull Collection<T> iterable);

    boolean retainAll(@Nonnull Collection<T> iterable);

    @Nonnull List<T> toList();

    @Nonnull Iterator<T> iterator();

}
