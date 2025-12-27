package com.threeamigos.common.util.implementations.persistence;

import com.threeamigos.common.util.interfaces.persistence.Persistable;
import com.threeamigos.common.util.interfaces.persistence.PersistablesCollector;
import org.jspecify.annotations.NonNull;

import java.util.*;

/**
 * An implementation of the {@link PersistablesCollector} interface.
 * When the application shuts down, it should persist all tracked Persistables.
 *
 * @author Stefano Reksten
 */
public class BasicPersistablesCollector implements PersistablesCollector {

    private static ResourceBundle bundle;

    private static ResourceBundle getBundle() {
        if (bundle == null) {
            bundle = ResourceBundle.getBundle("com.threeamigos.common.util.implementations.persistence.BasicPersistablesCollector.BasicPersistablesCollector");
        }
        return bundle;
    }

    private final Set<Persistable> persistables = new HashSet<>();

    protected BasicPersistablesCollector() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::persist));
    }

    protected BasicPersistablesCollector(final Persistable... persistables) {
        this();
        if (persistables == null) {
            throw new IllegalArgumentException(getBundle().getString("nullPersistablesProvided"));
        }
        for (Persistable persistable : persistables) {
            if (persistable == null) {
                throw new IllegalArgumentException(getBundle().getString("cannotAddNullPersistable"));
            }
        }
        Collections.addAll(this.persistables, persistables);
    }

    @Override
    public void add(final @NonNull Persistable persistable) {
        if (persistable == null) {
            throw new IllegalArgumentException(getBundle().getString("cannotAddNullPersistable"));
        }
        persistables.add(persistable);
    }

    @Override
    public void remove(final @NonNull Persistable persistable) {
        if (persistable == null) {
            throw new IllegalArgumentException(getBundle().getString("cannotRemoveNullPersistable"));
        }
        persistables.remove(persistable);
    }

    @Override
    public @NonNull Collection<Persistable> getPersistables() {
        return Collections.unmodifiableSet(persistables);
    }

    @Override
    public void persist() {
        persistables.forEach(Persistable::persist);
    }

}
