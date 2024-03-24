package com.threeamigos.common.util.implementations.json;

import com.threeamigos.common.util.interfaces.json.Json;
import com.threeamigos.common.util.interfaces.json.JsonAdapter;
import com.threeamigos.common.util.interfaces.json.JsonBuilder;

import java.util.HashMap;
import java.util.Map;

public class JsonBuilderImpl implements JsonBuilder {

    private final Map<Class<?>, JsonAdapter<?>> map = new HashMap<>();

    @Override
    public <C, A extends JsonAdapter<C>> JsonBuilder registerAdapter(final Class<C> clazz, final A adapter) {
        map.put(clazz, adapter);
        return this;
    }

    @Override
    public <T> Json<T> build(final Class<T> tClass) {
        return new JsonImpl<>(tClass, map);
    }

}
