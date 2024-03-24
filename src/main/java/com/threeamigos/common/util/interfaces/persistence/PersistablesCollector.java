package com.threeamigos.common.util.interfaces.persistence;

import java.util.Collection;

/**
 * An utility interface that helps keeping track of various {@link Persistable} objects.
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
    void add(Persistable persistable);

    void remove(Persistable persistable);

    Collection<Persistable> getPersistables();

}
