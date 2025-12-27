package com.threeamigos.common.util.interfaces.ui;

import org.jspecify.annotations.NonNull;

import java.util.Collection;

/**
 * An interface that collects Hints for an application.
 *
 * @param <T> type of hint (e.g., java.lang.String)
 */
public interface HintsCollector<T> {

    /**
     * @param hint a Hint to add
     */
    void addHint(@NonNull Hint<T> hint);

    /**
     * @param hints a collection of hints to add
     */
    void addHints(@NonNull Collection<Hint<T>> hints);

    /**
     * @param hintsProducer returns a collection of hints to add
     */
    void addHints(@NonNull HintsProducer<T> hintsProducer);

    /**
     * @return all hints collected so far
     */
    @NonNull Collection<Hint<T>> getHints();

}
