package com.threeamigos.common.util.implementations.persistence;

import com.threeamigos.common.util.interfaces.persistence.Persistable;
import com.threeamigos.common.util.interfaces.persistence.PersistablesCollector;

import java.util.*;

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
        this.persistables.addAll((List.of(persistables)));
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
