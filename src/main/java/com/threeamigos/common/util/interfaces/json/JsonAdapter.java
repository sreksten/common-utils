package com.threeamigos.common.util.interfaces.json;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;

/**
 * An interface able to serialize and deserialize an entity of type T. Based on Google's JSON classes.
 * @param <T> type of the entity
 *
 * @author Stefano Reksten
 */
public interface JsonAdapter<T> extends JsonSerializer<T>, JsonDeserializer<T> {

}
