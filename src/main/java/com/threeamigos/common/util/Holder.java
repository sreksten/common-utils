package com.threeamigos.common.util;

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

    public Holder(final T object) {
        this.object = object;
    }

    public T get() {
        return object;
    }

    public void set(T object) {
        this.object = object;
    }

}
