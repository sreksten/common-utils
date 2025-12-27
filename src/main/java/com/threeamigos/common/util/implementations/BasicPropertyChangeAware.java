package com.threeamigos.common.util.implementations;

import com.threeamigos.common.util.interfaces.PropertyChangeAware;
import org.jspecify.annotations.NonNull;

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

    protected void firePropertyChange(final @NonNull String propertyName) {
        propertyChangeSupport.firePropertyChange(propertyName, null, null);
    }

    protected void firePropertyChange(final @NonNull String propertyName, final Object oldValue, final Object newValue) {
        propertyChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }

    @Override
    public void addPropertyChangeListener(final @NonNull PropertyChangeListener pcl) {
        propertyChangeSupport.addPropertyChangeListener(pcl);
    }

    @Override
    public void removePropertyChangeListener(final @NonNull PropertyChangeListener pcl) {
        propertyChangeSupport.removePropertyChangeListener(pcl);
    }

}
