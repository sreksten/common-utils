package com.threeamigos.common.util.interfaces.persistence;

import jakarta.annotation.Nonnull;

import java.util.Collection;

/**
 * A utility interface that helps keep track of various {@link Persistable} objects.
 * PersistablesCollector is a Persistable itself. When the persist() method is called, it
 * should ask all tracked Persistables to persist.
 *
 * @author Stefano Reksten
 */
public interface PersistablesCollector extends Persistable {

    /**
     * Tracks a {@link Persistable}.
     *
     * @param persistable a Persistable to track
     */
    void add(@Nonnull Persistable persistable);

    void remove(@Nonnull Persistable persistable);

    @Nonnull Collection<Persistable> getPersistables();

}
