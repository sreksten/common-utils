package com.threeamigos.common.util.interfaces.ui;

/**
 * An interface that provides a single hint and tip for an application.
 *
 * @param <T> type of hint (e.g., java.lang.String)
 * @author Stefano Reksten
 */
@FunctionalInterface
public interface Hint<T> {

    /**
     * Retrieves the hint value.
     *
     * @return hint value
     */
    T getHint();
}
