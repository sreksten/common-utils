package com.threeamigos.common.util.implementations.preferences.file;

import com.threeamigos.common.util.interfaces.preferences.Preferences;
import jakarta.annotation.Nonnull;

import java.beans.PropertyChangeListener;

public class TestClassPreferences implements Preferences {

    @Override
    public void addPropertyChangeListener(@Nonnull PropertyChangeListener pcl) {
    }

    @Override
    public void removePropertyChangeListener(@Nonnull PropertyChangeListener pcl) {
    }

    @Override
    public @Nonnull String getDescription() {
        return "TestClassPreferences";
    }

    @Override
    public void loadDefaultValues() {
    }
}
