package com.threeamigos.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Holder unit test")
class HolderUnitTest {

    @Test
    @DisplayName("Should have working constructor")
    void shouldHaveWorkingConstructor() {
        // Given
        Holder<String> holder = new Holder<>("Test");
        // When
        String content = holder.get();
        // Then
        assertEquals("Test", content);
    }

    @Test
    @DisplayName("Should have working setter")
    void shouldHaveWorkingSetter() {
        // Given
        Holder<String> holder = new Holder<>();
        // When
        holder.set("Value");
        // Then
        assertEquals("Value", holder.get());
    }

}