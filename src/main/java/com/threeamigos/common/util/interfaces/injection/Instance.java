package com.threeamigos.common.util.interfaces.injection;

public interface Instance<T> extends Iterable<T> {
    T get() throws Exception;
}
