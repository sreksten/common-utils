package com.threeamigos.common.util.implementations.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName( "CustomResolution unit test")
class CustomResolutionUnitTest {

    @Test
    @DisplayName("Should return correct width")
    void shouldReturnCorrectWidth() {
        // Given
        CustomResolution sut = new CustomResolution(100, 200);
        // When
        int width = sut.getWidth();
        // Then
        assertEquals(100, width);
    }

    @Test
    @DisplayName("Should return correct height")
    void shouldReturnCorrectHeight() {
        // Given
        CustomResolution sut = new CustomResolution(100, 200);
        // When
        int height = sut.getHeight();
        // Then
        assertEquals(200, height);
    }

    @Test
    @DisplayName("Should return correct resolution string")
    void shouldReturnCorrectResolutionString() {
        // Given
        CustomResolution sut = new CustomResolution(100, 200);
        // When
        String resolution = sut.toString();
        // Then
        assertEquals("Custom (100 x 200)", resolution);
    }

    @Test
    @DisplayName("getName() should return same as toString()")
    void getNameShouldReturnSameAsToString() {
        // Given
        CustomResolution sut = new CustomResolution(100, 200);
        // When
        String resolution = sut.getName();
        // Then
        assertEquals("Custom (100 x 200)", resolution);
    }
}