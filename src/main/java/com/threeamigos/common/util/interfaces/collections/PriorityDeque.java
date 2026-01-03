package com.threeamigos.common.util.interfaces.collections;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.function.Function;

/**
 * A prioritized Deque. When polling, objects with higher priority are returned first.<br/>
 * You can, however, count or poll objects with a given priority using FIFO or LIFO policies.
 *
 * @param <T> type of the objects stored in the deque
 */
public interface PriorityDeque<T> {

    enum Policy {
        FIFO, // First-In-First-Out
        LIFO // Last-In-First-Out
    };

    /**
     * Sets the policy for polling objects from the deque.
     * The default policy is FIFO.
     * @param policy the policy to set
     */
    void setPolicy(Policy policy);

    /**
     * Gets the policy for polling objects from the deque.
     * @return the policy
     */
    Policy getPolicy();

    /**
     * Adds an object to the deque.
     * @param t object to add
     * @param priority priority of the object
     */
    void add(T t, int priority);

    /**
     * Retrieves, but does not remove, the head of this deque (with the highest priority).
     * @return the head of this deque, or null if empty
     */
    T peek();

    /**
     * Retrieves, but does not remove, the head of the highest priority bucket (FIFO).
     */
    T peekFifo();

    /**
     * Retrieves, but does not remove, the head of the highest priority bucket (LIFO).
     */
    T peekLifo();
    /**
     * Retrieves and removes an object with the highest priority from the deque.
     * The default policy is FIFO.
     * @return the oldest object with the highest priority
     */
    T poll();

    /**
     * Retrieves and removes an object with the highest priority from the deque using a FIFO policy.
     * @return the oldest object with the highest priority
     */
    T pollFifo();

    /**
     * Retrieves and removes an object with given priority from the deque using a FIFO policy.
     * @param priority priority of the object to retrieve
     * @return the oldest object with given priority
     */
    T pollFifo(int priority);

    /**
     * Retrieves and removes an object with the highest priority from the deque using a LIFO policy.
     * @return the newest object with the highest priority
     */
    T pollLifo();

    /**
     * Retrieves and removes an object with given priority from the deque using a LIFO policy.
     * @param priority priority of the object to retrieve
     * @return the newest object with given priority
     */
    T pollLifo(int priority);

    /**
     * @return true if no objects are stored, false otherwise
     */
    boolean isEmpty();

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
     * @param filteringFunction filtering function; if true, then the object is removed
     */
    void clear(Function<T, Boolean>filteringFunction);

    /**
     * @return the maximum priority between all objects in the deque.
     */
    int getHighestNotEmptyPriority();

    boolean contains(T t);

    boolean containsAll(Collection<T> iterable);

    boolean remove();

    boolean remove(T t);

    boolean removeAll(Collection<T> iterable);

    boolean retainAll(Collection<T> iterable);

    List<T> toList();

    Iterator<T> iterator();

}
