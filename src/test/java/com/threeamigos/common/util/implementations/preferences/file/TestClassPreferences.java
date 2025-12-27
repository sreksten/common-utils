package com.threeamigos.common.util.implementations.preferences.file;

import com.threeamigos.common.util.interfaces.preferences.Preferences;
import org.jspecify.annotations.NonNull;

import java.beans.PropertyChangeListener;

public class TestClassPreferences implements Preferences {

    @Override
    public void addPropertyChangeListener(@NonNull PropertyChangeListener pcl) {
    }

    @Override
    public void removePropertyChangeListener(@NonNull PropertyChangeListener pcl) {
    }

    @Override
    public @NonNull String getDescription() {
        return "TestClassPreferences";
    }

    @Override
    public void loadDefaultValues() {
    }
}
