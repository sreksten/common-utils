package com.threeamigos.common.util;

import org.jspecify.annotations.Nullable;

/**
 * A generic holder for objects of type T
 *
 * @param <T> type of object to hold
 * @author Stefano Reksten
 */
public class Holder<T> {

    private T object;

    public Holder() {
    }

    public Holder(final @Nullable T object) {
        this.object = object;
    }

    public @Nullable T get() {
        return object;
    }

    public void set(@Nullable T object) {
        this.object = object;
    }

}
