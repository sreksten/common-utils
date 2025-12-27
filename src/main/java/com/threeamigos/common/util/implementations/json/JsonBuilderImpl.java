package com.threeamigos.common.util.implementations.json;

import com.threeamigos.common.util.interfaces.json.Json;
import com.threeamigos.common.util.interfaces.json.JsonAdapter;
import com.threeamigos.common.util.interfaces.json.JsonBuilder;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * An implementation of the JsonBuilder interface.
 *
 * @author Stefano Reksten
 */
class JsonBuilderImpl implements JsonBuilder {

    private static ResourceBundle bundle;

    private static ResourceBundle getBundle() {
        if (bundle == null) {
            bundle = ResourceBundle.getBundle("com.threeamigos.common.util.implementations.json.JsonBuilderImpl.JsonBuilderImpl");
        }
        return bundle;
    }

    private final Map<Class<?>, JsonAdapter<?>> map = new HashMap<>();

    @Override
    public <C, A extends JsonAdapter<C>> JsonBuilder registerAdapter(final @NonNull Class<C> clazz, final @NonNull A adapter) {
        if (clazz == null) {
            throw new IllegalArgumentException(getBundle().getString("noClassProvided"));
        }
        if (adapter == null) {
            throw new IllegalArgumentException(String.format(getBundle().getString("noAdapterProvided"), clazz.getName()));
        }
        map.put(clazz, adapter);
        return this;
    }

    @Override
    public <T> Json<T> build(final @NonNull Class<T> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException(getBundle().getString("noClassProvided"));
        }
        return new JsonImpl<>(clazz, map);
    }

}
