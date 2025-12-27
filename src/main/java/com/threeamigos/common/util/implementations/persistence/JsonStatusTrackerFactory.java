package com.threeamigos.common.util.implementations.persistence;

import com.threeamigos.common.util.interfaces.json.Json;
import com.threeamigos.common.util.interfaces.persistence.StatusTracker;
import com.threeamigos.common.util.interfaces.persistence.StatusTrackerFactory;
import org.jspecify.annotations.NonNull;

import java.util.ResourceBundle;

/**
 * An implementation able to provide a state tracker for an object.
 *
 * @param <T>
 * @author Stefano Reksten
 */
public class JsonStatusTrackerFactory<T> implements StatusTrackerFactory<T> {

    private static ResourceBundle bundle;

    private static ResourceBundle getBundle() {
        if (bundle == null) {
            bundle = ResourceBundle.getBundle("com.threeamigos.common.util.implementations.persistence.JsonStatusTrackerFactory.JsonStatusTrackerFactory");
        }
        return bundle;
    }

    private final Json<T> json;

    public JsonStatusTrackerFactory(final @NonNull Json<T> json) {
        if (json == null) {
            throw new IllegalArgumentException(getBundle().getString("noJsonProvided"));
        }
        this.json = json;
    }

    @Override
    public StatusTracker<T> buildStatusTracker(final @NonNull T entity) {
        if (entity == null) {
            throw new IllegalArgumentException(getBundle().getString("noEntityProvided"));
        }
        return new JsonStatusTracker<>(entity, json);
    }
}
