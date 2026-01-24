package com.threeamigos.common.util.interfaces;

import jakarta.annotation.Nonnull;

import java.beans.PropertyChangeListener;

/**
 * An interface that is aware of PropertyChangeListeners bound to it.
 *
 * @author Stefano Reksten
 */
public interface PropertyChangeAware {

    void addPropertyChangeListener(@Nonnull PropertyChangeListener pcl);

    void removePropertyChangeListener(@Nonnull PropertyChangeListener pcl);

}
