package com.threeamigos.common.util.implementations.persistence;

import com.threeamigos.common.util.interfaces.persistence.Persistable;
import com.threeamigos.common.util.interfaces.persistence.PersistablesCollector;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * An implementation of the {@link PersistablesCollector} interface.
 *
 * @author Stefano Reksten
 */
public class BasicPersistablesCollector implements PersistablesCollector {

    private final Set<Persistable> persistables = new HashSet<>();

    protected BasicPersistablesCollector() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::persist));
    }

    protected BasicPersistablesCollector(final Persistable... persistables) {
        this();
        Collections.addAll(this.persistables, persistables);
    }

    @Override
    public void add(final Persistable persistable) {
        persistables.add(persistable);
    }

    @Override
    public void remove(final Persistable persistable) {
        persistables.remove(persistable);
    }

    @Override
    public Collection<Persistable> getPersistables() {
        return Collections.unmodifiableSet(persistables);
    }

    @Override
    public void persist() {
        persistables.forEach(Persistable::persist);
    }

}
