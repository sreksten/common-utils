package com.threeamigos.common.util.implementations.json;

import com.threeamigos.common.util.interfaces.json.JsonBuilder;

/**
 * Factory class for creating instances of JsonBuilder.
 *
 * @author Stefano Reksten
 */
public class JsonBuilderFactory {

    private JsonBuilderFactory() {
    }

    /**
     * Creates a new JsonBuilder instance.
     * @return a builder
     */
    public static JsonBuilder builder() {
        return new JsonBuilderImpl();
    }

}
