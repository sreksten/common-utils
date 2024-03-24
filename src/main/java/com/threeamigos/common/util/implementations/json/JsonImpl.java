package com.threeamigos.common.util.implementations.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.threeamigos.common.util.interfaces.json.Json;
import com.threeamigos.common.util.interfaces.json.JsonAdapter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class JsonImpl<T> implements Json<T> {

    private final Class<T> tClass;
    private final Map<Class<?>, JsonAdapter<?>> typeAdapters;

    JsonImpl(final Class<T> tClass, final Map<Class<?>, JsonAdapter<?>> typeAdapters) {
        this.tClass = tClass;
        this.typeAdapters = typeAdapters;
    }

    @Override
    public String toJson(final T entity) {
        return build().toJson(entity);
    }

    @Override
    public void toJson(final T entity, final OutputStream outputStream) throws IOException {
        outputStream.write(build().toJson(entity).getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }

    @Override
    public T fromJson(final String string) {
        return build().fromJson(string, tClass);
    }

    @Override
    public T fromJson(final InputStream inputStream) throws IOException {
        return build().fromJson(toJsonRepresentation(inputStream), tClass);
    }

    private Gson build() {
        GsonBuilder builder = new GsonBuilder();
        registerAdapters(builder);
        return builder.create();
    }

    @Override
    public void fromJson(final String string, final T entity) {
        buildWithTypeAdapter(entity).fromJson(string, entity.getClass());
    }

    @Override
    public void fromJson(final InputStream inputStream, final T entity) throws IOException {
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

    private final void registerAdapters(final GsonBuilder builder) {
        for (Map.Entry<Class<?>, JsonAdapter<?>> adapter : typeAdapters.entrySet()) {
            builder.registerTypeAdapter(adapter.getKey(), adapter.getValue());
        }
    }

}
