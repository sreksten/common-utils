package com.threeamigos.common.util.implementations.ui;

import com.threeamigos.common.util.interfaces.ui.Hint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("JHintArea unit test")
class JHintAreaUnitTest {

    @Test
    @DisplayName("Should have a working constructor")
    void shouldHaveWorkingConstructor() {
        // Given
        Hint<String> hint = new StringHint("A hint");
        // When
        JHintArea sut = new JHintArea(hint);
        // Then
        assertEquals("A hint", sut.getText());
    }

    @Test
    @DisplayName("Should have a working setter")
    void shouldHAveWorkingSetter() {
        // Given
        Hint<String> hint = new StringHint("Another hint");
        JHintArea sut = new JHintArea(new StringHint("My test"));
        // When
        sut.setHint(hint);
        // Then
        assertEquals("Another hint", sut.getText());
    }

}