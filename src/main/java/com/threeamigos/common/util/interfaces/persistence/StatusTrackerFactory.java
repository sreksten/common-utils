package com.threeamigos.common.util.interfaces.persistence;

import jakarta.annotation.Nonnull;

/**
 * An interface able to provide status trackers for objects.
 *
 * @param <T>
 * @author Stefano Reksten
 */
@FunctionalInterface
public interface StatusTrackerFactory<T> {

    StatusTracker<T> buildStatusTracker(@Nonnull T entity);

}
