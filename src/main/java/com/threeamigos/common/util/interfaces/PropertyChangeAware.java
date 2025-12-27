package com.threeamigos.common.util.interfaces;

import org.jspecify.annotations.NonNull;

import java.beans.PropertyChangeListener;

/**
 * An interface that is aware of PropertyChangeListeners bound to it.
 *
 * @author Stefano Reksten
 */
public interface PropertyChangeAware {

    void addPropertyChangeListener(@NonNull PropertyChangeListener pcl);

    void removePropertyChangeListener(@NonNull PropertyChangeListener pcl);

}
