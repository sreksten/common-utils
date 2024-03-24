package com.threeamigos.common.util.implementations.preferences.file;

import com.threeamigos.common.util.interfaces.preferences.Preferences;

import java.beans.PropertyChangeListener;

public class TestClassPreferences implements Preferences {

    @Override
    public void addPropertyChangeListener(PropertyChangeListener pcl) {
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener pcl) {
    }

    @Override
    public String getDescription() {
        return "TestClassPreferences";
    }

    @Override
    public void validate() {

    }

    @Override
    public void loadDefaultValues() {

    }
}
