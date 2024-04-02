package com.threeamigos.common.util.implementations.preferences.flavours;

import com.threeamigos.common.util.interfaces.preferences.flavours.HintsPreferences;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HintsPreferencesImpl unit test")
@Tag("unit")
@Tag("preferences")
class HintsPreferencesImplUnitTest {

    @Test
    @DisplayName("Should load default values")
    void shouldLoadDefaultValues() {
        // Given
        HintsPreferences sut = new HintsPreferencesImpl("Preferences property name");
        // When
        sut.loadDefaultValues();
        // Then
        assertEquals(HintsPreferences.HINTS_PREFERENCES_VISIBLE_DEFAULT, sut.isHintsVisibleAtStartup());
        assertEquals(HintsPreferences.HINTS_PREFERENCES_INDEX_DEFAULT, sut.getLastHintIndex());
    }

    @Test
    @DisplayName("Should validate")
    void shouldValidate() {
        // Given
        HintsPreferences sut = new HintsPreferencesImpl("Preferences property name");
        // Then
        assertDoesNotThrow(sut::validate);
    }

    @Test
    @DisplayName("Should have a property description")
    void shouldHavePropertyDescription() {
        // Given
        HintsPreferences sut = new HintsPreferencesImpl("Preferences property name");
        // When
        String description = sut.getDescription();
        // Then
        assertFalse(description.isEmpty());
    }

}