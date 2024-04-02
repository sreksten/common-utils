package com.threeamigos.common.util.implementations.ui;

import com.threeamigos.common.util.Holder;
import com.threeamigos.common.util.implementations.preferences.flavours.HintsPreferencesImpl;
import com.threeamigos.common.util.interfaces.preferences.flavours.HintsPreferences;
import com.threeamigos.common.util.interfaces.ui.Hint;
import com.threeamigos.common.util.interfaces.ui.HintsCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("HintsSupport unit test")
@Tag("unit")
@Tag("hints")
class HintsSupportUnitTest {

    private HintsPreferences hintsPreferences;
    private HintsCollector<String> hintsCollector;

    @BeforeEach
    void setup() {
        Hint<String> firstHint = new StringHint("First hint");
        Hint<String> secondHint = new StringHint("Second hint");
        Hint<String> thirdHint = new StringHint("Third hint");
        Collection<Hint<String>> hints = new ArrayList<>();
        hints.add(firstHint);
        hints.add(secondHint);
        hints.add(thirdHint);
        hintsPreferences = new HintsPreferencesImpl("Preferences changed");
        hintsCollector = new HintsCollectorImpl<>();
        hintsCollector.addHints(hints);
    }

    @Test
    @DisplayName("Should get first hint at first launch")
    void shouldGetFirstHintAtFirstLaunch() {
        // Given
        HintsSupport sut = new HintsSupport(hintsPreferences, hintsCollector);
        // When
        Hint<String> hint = sut.getNextHint();
        // Then
        assertEquals("First hint", hint.getHint());
    }

    @Test
    @DisplayName("Should get next hint")
    void shouldGetNextHint() {
        // Given
        hintsPreferences.setLastHintIndex(1);
        HintsSupport sut = new HintsSupport(hintsPreferences, hintsCollector);
        // When
        Hint<String> hint = sut.getNextHint();
        // Then
        assertEquals("Third hint", hint.getHint());
    }

    @Test
    @DisplayName("Should get previous hint")
    void shouldGetPreviousHint() {
        // Given
        HintsSupport sut = new HintsSupport(hintsPreferences, hintsCollector);
        // When
        Hint<String> hint = sut.getPreviousHint();
        // Then
        assertEquals("Third hint", hint.getHint());
    }

    @Test
    @DisplayName("Should track current preference")
    void shouldTrackCurrentPreference() {
        // Given
        HintsSupport sut = new HintsSupport(hintsPreferences, hintsCollector);
        // When
        sut.getPreviousHint();
        // Then
        assertEquals(2, hintsPreferences.getLastHintIndex());
    }

    @Test
    @DisplayName("Should fire property change")
    void shouldFirePropertyChange() {
        // Given
        HintsSupport sut = new HintsSupport(hintsPreferences, hintsCollector);
        Holder<String> stringHolder = new Holder<>();
        PropertyChangeListener pcl = evt -> stringHolder.set(evt.getPropertyName());
        hintsPreferences.addPropertyChangeListener(pcl);
        // When
        sut.getNextHint();
        // Then
        assertEquals("Preferences changed", stringHolder.get());
    }

    @Test
    @DisplayName("Should remember if hints are visible at startup")
    void shoudRememberIfVisibleAtStartup() {
        // Given
        HintsSupport sut = new HintsSupport(hintsPreferences, hintsCollector);
        // When
        sut.setHintsVisibleAtStartup(true);
        // Then
        assertTrue(sut.isHintsVisibleAtStartup());
    }

    @Test
    @DisplayName("Should start from first hint if preference index is invalid at start")
    void shouldStartFromFirstHintIfPreferencesIndexIsInvalidAtStart() {
        // Given
        HintsSupport sut = new HintsSupport(hintsPreferences, hintsCollector);
        hintsPreferences.setLastHintIndex(5);
        // When
        sut.getNextHint();
        // Then
        assertEquals(0, sut.getCurrentHintIndex());
    }

    @Test
    @DisplayName("Should start from first hint if preference index is invalid at runtime")
    void shouldStartFromFirstHintIfPreferencesIndexIsInvalidAtRuntime() {
        // Given
        hintsPreferences.setLastHintIndex(5);
        HintsSupport sut = new HintsSupport(hintsPreferences, hintsCollector);
        // When
        sut.getNextHint();
        // Then
        assertEquals(0, sut.getCurrentHintIndex());
    }

    @Test
    @DisplayName("Should hint that no hints are available if so")
    void shouldHintThatNoHintsAreAvailableIfSo() {
        // Given
        HintsCollector<String> emptyHintsCollector = new HintsCollectorImpl<>();
        HintsSupport sut = new HintsSupport(hintsPreferences, emptyHintsCollector);
        // When
        int totalHints = sut.getTotalHints();
        // Then
        assertEquals(1, totalHints);
        assertEquals("No hints were provided.", sut.getNextHint().getHint());
    }

}