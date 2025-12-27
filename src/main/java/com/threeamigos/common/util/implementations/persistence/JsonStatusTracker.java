package com.threeamigos.common.util.implementations.persistence;

import com.threeamigos.common.util.interfaces.json.Json;
import com.threeamigos.common.util.interfaces.persistence.StatusTracker;
import org.jspecify.annotations.NonNull;

import java.util.ResourceBundle;

/**
 * A class implementing the {@link StatusTracker} interface using JSON
 * (de)serialization of the tracked object.
 *
 * @param <T>
 * @author Stefano Reksten
 */
public class JsonStatusTracker<T> implements StatusTracker<T> {

    private static ResourceBundle bundle;

    private static ResourceBundle getBundle() {
        if (bundle == null) {
            bundle = ResourceBundle.getBundle("com.threeamigos.common.util.implementations.persistence.JsonStatusTracker.JsonStatusTracker");
        }
        return bundle;
    }

    private final T entity;
    private final Json<T> json;

    private String initialEntityRepresentationAsString;

    JsonStatusTracker(final @NonNull T entity, final @NonNull Json<T> json) {
        if (entity == null) {
            throw new IllegalArgumentException(getBundle().getString("noEntityProvided"));
        }
        if (json == null) {
            throw new IllegalArgumentException(getBundle().getString("noJsonProvided"));
        }
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

    @NonNull String getEntityRepresentationAsString() {
        return json.toJson(entity);
    }

}
