package com.threeamigos.common.util.interfaces;

import java.beans.PropertyChangeListener;

/**
 * An interface that is aware of PropertyChangeListeners bound to it.
 *
 * @author Stefano Reksten
 */
public interface PropertyChangeAware {

	public void addPropertyChangeListener(PropertyChangeListener pcl);

	public void removePropertyChangeListener(PropertyChangeListener pcl);

}
