package com.threeamigos.common.util.interfaces.ui;

import jakarta.annotation.Nonnull;

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
    void addHint(@Nonnull Hint<T> hint);

    /**
     * @param hints a collection of hints to add
     */
    void addHints(@Nonnull Collection<Hint<T>> hints);

    /**
     * @param hintsProducer returns a collection of hints to add
     */
    void addHints(@Nonnull HintsProducer<T> hintsProducer);

    /**
     * @return all hints collected so far
     */
    @Nonnull Collection<Hint<T>> getHints();

}
