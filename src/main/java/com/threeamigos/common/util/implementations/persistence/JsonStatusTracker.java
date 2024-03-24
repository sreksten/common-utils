package com.threeamigos.common.util.implementations.persistence;

import com.threeamigos.common.util.interfaces.json.Json;
import com.threeamigos.common.util.interfaces.persistence.StatusTracker;

/**
 * A class implementing the {@link StatusTracker} interface using Json
 * (de)serialization of the tracked object.
 *
 * @param <T>
 * @author Stefano Reksten
 */
public class JsonStatusTracker<T> implements StatusTracker<T> {

    private final T entity;
    private final Json<T> json;

    private String initialEntityRepresentationAsString;

    JsonStatusTracker(final T entity, final Json<T> json) {
        this.entity = entity;
        this.json = json;
    }

    @Override
    public void loadInitialValues() {
        initialEntityRepresentationAsString = getEntityRepresentationAsString();
    }

    @Override
    public boolean hasChanged() {
        return !getEntityRepresentationAsString().equals(initialEntityRepresentationAsString);
    }

    String getEntityRepresentationAsString() {
        return json.toJson(entity);
    }

}
