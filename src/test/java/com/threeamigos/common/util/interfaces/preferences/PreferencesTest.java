package com.threeamigos.common.util.interfaces.preferences;

import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.beans.PropertyChangeListener;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName( "Preferences unit test")
class PreferencesTest {

    @Test
    @DisplayName("Standard validate method should not throw exception for valid preferences")
    void shouldNotThrowException() {
        // Given
        Preferences preferences = new Preferences() {
            @Override
            public void addPropertyChangeListener(@Nonnull PropertyChangeListener pcl) {
            }

            @Override
            public void removePropertyChangeListener(@Nonnull PropertyChangeListener pcl) {
            }

            @Override
            public @Nonnull String getDescription() {
                return "Test preferences";
            }

            @Override
            public void loadDefaultValues() {
            }
        };
        // Then
        assertDoesNotThrow(preferences::validate);
    }
}
