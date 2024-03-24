package com.threeamigos.common.util.implementations.persistence;

import com.threeamigos.common.util.interfaces.json.Json;
import com.threeamigos.common.util.interfaces.persistence.StatusTracker;
import com.threeamigos.common.util.interfaces.persistence.StatusTrackerFactory;

/**
 * An interface able to provide a state tracker for an object.
 *
 * @param <T>
 * @author Stefano Reksten
 */
public class JsonStatusTrackerFactory<T> implements StatusTrackerFactory<T> {

    private final Json<T> json;

    public JsonStatusTrackerFactory(final Json<T> json) {
        this.json = json;
    }

    @Override
    public StatusTracker<T> buildStatusTracker(final T entity) {
        return new JsonStatusTracker<>(entity, json);
    }
}
