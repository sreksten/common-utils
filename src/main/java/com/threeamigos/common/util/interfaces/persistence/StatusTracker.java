package com.threeamigos.common.util.interfaces.persistence;

/**
 * An interface that tracks state changes of an object.
 *
 * @param <T>
 * @author Stefano Reksten
 */
public interface StatusTracker<T> {

    /**
     * Asks the object to load its initial (default) values.
     */
    void loadInitialValues();

    /**
     * @return true if the state of the tracked object has changed.
     */
    boolean hasChanged();

}
