package com.threeamigos.common.util.interfaces.collections;

/**
 * A prioritized Deque. When polling, objects with higher priority are returned first.<br/>
 * You can, however, count or poll objects with a given priority using FIFO or LIFO policies.
 *
 * @param <T> type of the objects stored in the deque
 */
public interface PriorityDeque<T> {

    /**
     * @return the maximum priority between all objects in the deque.
     */
    int getHighestNotEmptyPriority();

    /**
     * Adds an object to the deque.
     * @param t object to add
     * @param priority priority of the object
     */
    void add(T t, int priority);

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
}
