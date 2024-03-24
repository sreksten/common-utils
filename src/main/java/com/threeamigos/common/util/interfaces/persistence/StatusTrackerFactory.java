package com.threeamigos.common.util.interfaces.persistence;

/**
 * An interface able to provide state trackers for objects.
 *
 * @param <T>
 * @author Stefano Reksten
 */
public interface StatusTrackerFactory<T> {

	public StatusTracker<T> buildStatusTracker(T entity);

}
