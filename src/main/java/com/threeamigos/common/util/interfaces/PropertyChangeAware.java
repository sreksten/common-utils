package com.threeamigos.common.util.interfaces;

import java.beans.PropertyChangeListener;

/**
 * An interface that is aware of PropertyChangeListeners bound to it.
 *
 * @author Stefano Reksten
 */
public interface PropertyChangeAware {

    void addPropertyChangeListener(PropertyChangeListener pcl);

    void removePropertyChangeListener(PropertyChangeListener pcl);

}
