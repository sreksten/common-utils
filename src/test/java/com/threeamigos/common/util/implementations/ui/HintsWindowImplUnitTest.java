package com.threeamigos.common.util.implementations.ui;

import com.threeamigos.common.util.implementations.preferences.flavours.HintsPreferencesImpl;
import com.threeamigos.common.util.interfaces.preferences.flavours.HintsPreferences;
import com.threeamigos.common.util.interfaces.ui.Hint;
import com.threeamigos.common.util.interfaces.ui.HintsCollector;
import com.threeamigos.common.util.interfaces.ui.HintsDisplayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@DisplayName("HintsWindowImpl unit test")
@Tag("unit")
class HintsWindowImplUnitTest {

    private HintsPreferences hintsPreferences;
    private HintsSupport hintsSupport;

    @BeforeEach
    void setup() {
        Collection<Hint<String>> hints = List.of(new StringHint("First hint"), new StringHint("Second hint"), new StringHint("Third hint"));
        hintsPreferences = new HintsPreferencesImpl("hints preferences");
        HintsCollector<String> hintsCollector = new HintsCollectorImpl<>();
        hintsCollector.addHints(hints);
        hintsSupport = new HintsSupport(hintsPreferences, hintsCollector);
    }

    @Test
    @DisplayName("Should open a hint window")
    @EnabledIfEnvironmentVariable(named = "AWT_TESTS", matches = "true", disabledReason = "AWT_TESTS is not true")
    @ResourceLock(value = "java.lang.System#properties", mode = ResourceAccessMode.READ_WRITE)
    void shouldOpenHintWindow() {
        // Given
        hintsPreferences.setLastHintIndex(-1);
        HintsDisplayer sut = new HintsWindowImpl("Application name", hintsSupport);
        // When
        sut.showHints(null);
        // Then
        assertNotEquals(-1, hintsSupport.getCurrentHintIndex());
    }

    @Test
    @DisplayName("Should go to previous hint")
    void shouldGoToPreviousHint() {
        // Given
        HintsWindowImpl sut = new HintsWindowImpl("Application name", hintsSupport);
        hintsPreferences.setLastHintIndex(1);
        // When
        sut.goToPreviousHint();
        // Then
        assertEquals(0, hintsSupport.getCurrentHintIndex());
        assertEquals("(1/3)", sut.getIndexLabelText());
        assertEquals("First hint", sut.getHintText());
    }

    @Test
    @DisplayName("Should go to next hint")
    void shouldGoToNextHint() {
        // Given
        HintsWindowImpl sut = new HintsWindowImpl("Application name", hintsSupport);
        hintsPreferences.setLastHintIndex(1);
        // When
        sut.goToNextHint();
        // Then
        assertEquals(2, hintsSupport.getCurrentHintIndex());
        assertEquals("(3/3)", sut.getIndexLabelText());
        assertEquals("Third hint", sut.getHintText());
    }
}
