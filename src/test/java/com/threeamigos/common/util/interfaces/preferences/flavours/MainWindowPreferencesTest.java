package com.threeamigos.common.util.interfaces.preferences.flavours;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("MainWindowPreferences")
@Tag("preferences")
class MainWindowPreferencesTest {

    @Test
    @DisplayName("Should return a valid preferences description")
    void shouldReturnValidPreferencesDescription() {
        // Given
        MainWindowPreferences sut = mock(MainWindowPreferences.class);
        when(sut.getDescription()).thenCallRealMethod();
        // When
        String description = sut.getDescription();
        // Then
        Assertions.assertNotNull(description);
        Assertions.assertFalse(description.isEmpty());
        Assertions.assertFalse(description.isBlank());
    }
}
