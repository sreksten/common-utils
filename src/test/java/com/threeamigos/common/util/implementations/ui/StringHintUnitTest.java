package com.threeamigos.common.util.implementations.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("StringHint unit test")
@Tag("unit")
@Tag("hints")
class StringHintUnitTest {

    @Test
    @DisplayName("Should preserve hint")
    void shouldPreserveHint() {
        // Given
        String hintValue = "A hint";
        // When
        StringHint sut = new StringHint(hintValue);
        // Then
        assertEquals(hintValue, sut.getHint());
    }
}