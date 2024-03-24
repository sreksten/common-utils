package com.threeamigos.common.util.interfaces.persistence;

/**
 * An interface able to provide state trackers for objects.
 *
 * @param <T>
 * @author Stefano Reksten
 */
@FunctionalInterface
public interface StatusTrackerFactory<T> {

    StatusTracker<T> buildStatusTracker(T entity);

}
