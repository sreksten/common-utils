package com.threeamigos.common.util.implementations.ui;

import com.threeamigos.common.util.interfaces.ui.Hint;
import com.threeamigos.common.util.interfaces.ui.HintsCollector;
import com.threeamigos.common.util.interfaces.ui.HintsProducer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("HintsCollectorImpl unit test")
@Tag("unit")
@Tag("hints")
class HintsCollectorImplUnitTest {

    @Test
    @DisplayName("Should track a hint")
    void shouldTrackHint() {
        // Given
        HintsCollector<String> sut = new HintsCollectorImpl<>();
        Hint<String> hint = new StringHint("A hint");
        // When
        sut.addHint(hint);
        // Then
        assertTrue(sut.getHints().contains(hint));
    }

    @Test
    @DisplayName("Should track hints from a collection")
    void shouldTrackHintsFromACollection() {
        // Given
        HintsCollector<String> sut = new HintsCollectorImpl<>();
        Collection<Hint<String>> hints = List.of(new StringHint("A hint"), new StringHint("Another hint"));
        // When
        sut.addHints(hints);
        // Then
        assertThat(sut.getHints(), containsInAnyOrder(hints.toArray()));
    }

    @Test
    @DisplayName("Should track hints from a Producer")
    void shouldTrackHintsFromAProducer() {
        // Given
        HintsCollector<String> sut = new HintsCollectorImpl<>();
        final Collection<Hint<String>> hints = List.of(new StringHint("A hint"), new StringHint("Another hint"));
        HintsProducer<String> producer = new HintsProducer<String>() {
            @Override
            public Collection<Hint<String>> getHints() {
                return hints;
            }
        };
        // When
        sut.addHints(producer);
        // Then
        assertThat(sut.getHints(), containsInAnyOrder(hints.toArray()));
    }

}