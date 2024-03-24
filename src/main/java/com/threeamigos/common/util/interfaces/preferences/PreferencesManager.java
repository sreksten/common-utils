package com.threeamigos.common.util.interfaces.preferences;

import com.threeamigos.common.util.interfaces.persistence.Persistable;

/**
 * A class that takes care of the lifecycle of a set of {@link Preferences}. Extending
 * {@link Persistable}, when the persist() method is called, it should result in the set
 * of preferences being saved.
 *
 * @param <T>
 * @author Stefano Reksten
 */
public interface PreferencesManager<T extends Preferences> extends Persistable {

}
