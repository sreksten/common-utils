package com.threeamigos.common.util.interfaces.json;

import jakarta.annotation.Nonnull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * An interface able to convert an entity to its JSON representation and vice versa. To build an instance of this
 * converter, see the {@link JsonBuilder} interface. Based on Google's JSON classes.
 *
 * @param <T> type of the entity
 *
 * @author Stefano Reksten
 */
public interface Json<T> {

    /**
     * Given an entity, returns its JSON representation.
     *
     * @param entity the entity to represent in a JSON format
     * @return JSON entity representation as a string
     */
    String toJson(@Nonnull T entity);

    /**
     * Given an entity, writes its JSON representation to an OutputStream.
     *
     * @param entity the entity to represent in a JSON format
     * @param outputStream destination
     */
    void toJson(@Nonnull T entity, @Nonnull OutputStream outputStream) throws IOException;

    /**
     * Given a JSON string, returns an entity of type T populated using values in its JSON representation.
     *
     * @param string the JSON representation of the entity
     * @return an instance of T populated using values in its JSON representation
     */
    T fromJson(@Nonnull String string);

    /**
     * Given an InputStream, retrieves an entity of type T populated using values in its JSON representation.
     * Based on Google's JSON classes.
     *
     * @param inputStream source of data
     * @return an entity populated using values in its JSON representation
     */
    T fromJson(@Nonnull InputStream inputStream) throws IOException;

    /**
     * Given an entity and a String, populates that entity fetching data
     * from a JSON representation contained in the string.
     *
     * @param json source of data
     * @param entity entity to populate
     */
    void fromJson(@Nonnull String json, @Nonnull T entity);

    /**
     * Given an entity and an InputStream, populates the entity fetching
     * data from the InputStream that should contain a JSON representation.
     *
     * @param inputStream source of data
     * @param entity      entity to populate
     */
    void fromJson(@Nonnull InputStream inputStream, @Nonnull T entity) throws IOException;

}
