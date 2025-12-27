package com.threeamigos.common.util.interfaces.persistence;

import org.jspecify.annotations.NonNull;

/**
 * An interface able to provide status trackers for objects.
 *
 * @param <T>
 * @author Stefano Reksten
 */
@FunctionalInterface
public interface StatusTrackerFactory<T> {

    StatusTracker<T> buildStatusTracker(@NonNull T entity);

}
