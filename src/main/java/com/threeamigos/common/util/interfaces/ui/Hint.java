package com.threeamigos.common.util.interfaces.ui;

/**
 * An interface that provide a single hint and tip for an application.
 *
 * @param <T> type of hint (e.g. java.lang.String)
 * @author Stefano Reksten
 */
public interface Hint<T> {

    public T getHint();
}
