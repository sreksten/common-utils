package com.threeamigos.common.util.implementations.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.threeamigos.common.util.interfaces.json.Json;
import com.threeamigos.common.util.interfaces.json.JsonAdapter;
import org.jspecify.annotations.NonNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * An implementation of the {@link Json} interface for a specific entity type.
 * @param <T> type of the entity
 *
 * @author Stefano Reksten
 */
class JsonImpl<T> implements Json<T> {

    private static ResourceBundle bundle;

    private static ResourceBundle getBundle() {
        if (bundle == null) {
            bundle = ResourceBundle.getBundle("com.threeamigos.common.util.implementations.json.JsonImpl.JsonImpl");
        }
        return bundle;
    }

    private final Class<T> tClass;
    private final Map<Class<?>, JsonAdapter<?>> typeAdapters;

    JsonImpl(final @NonNull Class<T> clazz, final @NonNull Map<Class<?>, JsonAdapter<?>> typeAdapters) {
        if (clazz == null) {
            throw new IllegalArgumentException(getBundle().getString("noClassProvided"));
        }
        if (typeAdapters == null) {
            throw new IllegalArgumentException(String.format(getBundle().getString("noTypeAdaptersProvided"), clazz.getName()));
        }
        this.tClass = clazz;
        this.typeAdapters = typeAdapters;
    }

    @Override
    public String toJson(final @NonNull T entity) {
        if (entity == null) {
            throw new IllegalArgumentException(getBundle().getString("noEntityProvided"));
        }
        return build().toJson(entity);
    }

    @Override
    public void toJson(final @NonNull T entity, final @NonNull OutputStream outputStream) throws IOException {
        if (entity == null) {
            throw new IllegalArgumentException(getBundle().getString("noEntityProvided"));
        }
        if (outputStream == null) {
            throw new IllegalArgumentException(getBundle().getString("noOutputStreamProvided"));
        }
        outputStream.write(build().toJson(entity).getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }

    @Override
    public T fromJson(final @NonNull String string) {
        if (string == null) {
            throw new IllegalArgumentException(getBundle().getString("noJsonProvided"));
        }
        return build().fromJson(string, tClass);
    }

    @Override
    public T fromJson(final @NonNull InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException(getBundle().getString("noInputStreamProvided"));
        }
        return build().fromJson(toJsonRepresentation(inputStream), tClass);
    }

    private Gson build() {
        GsonBuilder builder = new GsonBuilder();
        registerAdapters(builder);
        return builder.create();
    }

    @Override
    public void fromJson(final @NonNull String string, final @NonNull T entity) {
        if (string == null) {
            throw new IllegalArgumentException(getBundle().getString("noJsonProvided"));
        }
        if (entity == null) {
            throw new IllegalArgumentException(getBundle().getString("noEntityProvided"));
        }
        buildWithTypeAdapter(entity).fromJson(string, entity.getClass());
    }

    @Override
    public void fromJson(final @NonNull InputStream inputStream, final @NonNull T entity) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException(getBundle().getString("noInputStreamProvided"));
        }
        if (entity == null) {
            throw new IllegalArgumentException(getBundle().getString("noEntityProvided"));
        }
        fromJson(toJsonRepresentation(inputStream), entity);
    }

    private String toJsonRepresentation(final InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

    private Gson buildWithTypeAdapter(final T entity) {
        GsonBuilder builder = new GsonBuilder();
        registerAdapters(builder);
        builder.registerTypeAdapter(entity.getClass(), (InstanceCreator<T>) type -> entity);
        return builder.create();
    }

    private void registerAdapters(final GsonBuilder builder) {
        for (Map.Entry<Class<?>, JsonAdapter<?>> adapter : typeAdapters.entrySet()) {
            builder.registerTypeAdapter(adapter.getKey(), adapter.getValue());
        }
    }
}
