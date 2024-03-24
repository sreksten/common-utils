package com.threeamigos.common.util.implementations;

import com.threeamigos.common.util.interfaces.PropertyChangeAware;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * An implementation of the {@link PropertyChangeAware} interface.
 *
 * @author Stefano Reksten
 */
public class BasicPropertyChangeAware implements PropertyChangeAware {

    /**
     * transient to make Gson serializer ignore this field
     */
    private final transient PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this); //NOSONAR

    protected void firePropertyChange(final String propertyName) {
        propertyChangeSupport.firePropertyChange(propertyName, null, null);
    }

    protected void firePropertyChange(final String propertyName, final Object oldValue, final Object newValue) {
        propertyChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }

    @Override
    public void addPropertyChangeListener(final PropertyChangeListener pcl) {
        propertyChangeSupport.addPropertyChangeListener(pcl);
    }

    @Override
    public void removePropertyChangeListener(final PropertyChangeListener pcl) {
        propertyChangeSupport.removePropertyChangeListener(pcl);
    }

}
