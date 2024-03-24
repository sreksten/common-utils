package com.threeamigos.common.util.interfaces.preferences;

import com.threeamigos.common.util.interfaces.PropertyChangeAware;

/**
 * A set of preferences for part of an application. Extending {@link PropertyChangeAware},
 * it should inform each PropertyChangeListener bound to it of changes in its state.
 *
 * @author Stefano Reksten
 */
public interface Preferences extends PropertyChangeAware {

    /**
     * @return a description of this set of preferences.
     */
    public String getDescription();

    /**
     * Checks if the preferences are in a valid state.
     * If not it may throw a RuntimeException.
     */
    public void validate();

    /**
     * Loads the default values for this set of preferences.
     */
    public void loadDefaultValues();

}
