package com.threeamigos.common.util.interfaces.json;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * An interface used to pass from an entity to its Json representation and vice versa.
 *
 * @param <T> type of entity
 */
public interface Json<T> {

    /**
     * Given an entity returns its Json representation
     *
     * @param entity the entity to represent
     * @return Json entity representation as a string
     */
    public String toJson(T entity);

    /**
     * Given an entity writes its Json representation to an OutputStream
     *
     * @param entity       the entity to represent
     * @param outputStream destination
     * @throws IOException
     */
    public void toJson(T entity, OutputStream outputStream) throws IOException;

    /**
     * Given a Json string returns an entity
     *
     * @param string the Json representation of the entity
     * @return
     */
    public T fromJson(String string);

    /**
     * Given an InputStream retrieves an entity
     *
     * @param inputStream source
     * @return an entity
     * @throws IOException
     */
    public T fromJson(InputStream inputStream) throws IOException;

    /**
     * Given an entity and a String, populates that entity fetching data
     * from a Json representation contained in the string
     *
     * @param json   source of data
     * @param entity entity to populate
     */
    public void fromJson(String json, T entity);

    /**
     * Given an entity and an InputStream, populates the entity fetching
     * data from the InputStream that should contain a Json representation
     *
     * @param inputStream source of data
     * @param entity      entity to populate
     * @throws IOException
     */
    public void fromJson(InputStream inputStream, T entity) throws IOException;

}
