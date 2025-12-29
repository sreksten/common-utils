package com.threeamigos.common.util.implementations.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ResolutionEnum unit test")
class ResolutionEnumUnitTest {

    @ParameterizedTest
    @DisplayName("Should return correct resolution name")
    @MethodSource("resolutionProvider")
    void shouldReturnCorrectResolutionName(String enumName, String resolutionName, int width, int height) {
        // Given
        ResolutionEnum resolutionEnum = ResolutionEnum.valueOf(enumName);
        // When
        String resolutionDescription = resolutionEnum.getName();
        // Then
        assertEquals(resolutionName, resolutionDescription);
    }

    @ParameterizedTest
    @DisplayName("Should return correct resolution width")
    @MethodSource("resolutionProvider")
    void shouldReturnCorrectResolutionWidth(String enumName, String resolutionName, int width, int height) {
        // Given
        ResolutionEnum resolutionEnum = ResolutionEnum.valueOf(enumName);
        // When
        int resolutionWidth = resolutionEnum.getWidth();
        // Then
        assertEquals(width, resolutionWidth);
    }

    @ParameterizedTest
    @DisplayName("Should return correct resolution height")
    @MethodSource("resolutionProvider")
    void shouldReturnCorrectResolutionHeight(String enumName, String resolutionName, int width, int height) {
        // Given
        ResolutionEnum resolutionEnum = ResolutionEnum.valueOf(enumName);
        // When
        int resolutionHeight = resolutionEnum.getHeight();
        // Then
        assertEquals(height, resolutionHeight);
    }

    @ParameterizedTest
    @DisplayName("toString() should return name and resolution")
    @MethodSource("resolutionProvider")
    void toStringShouldReturnNameAndResolution(String enumName, String resolutionName, int width, int height) {
        // Given
        ResolutionEnum resolutionEnum = ResolutionEnum.valueOf(enumName);
        // When
        String resolution = resolutionEnum.toString();
        String expectedResolution = resolutionName + " (" + width + " x " + height + ")";
        // Then
        assertEquals(expectedResolution, resolution);
    }

    private static Stream<Arguments> resolutionProvider() {
        return Stream.of(
                Arguments.of("FULL_ULTRA_HD", "Full Ultra HD/8K", 7680, 4320),
                Arguments.of("ULTRA_HD", "Ultra HD/4K", 3840, 2160),
                Arguments.of("QUAD_HD", "Quad HD", 2560, 1440),
                Arguments.of("FULL_HD", "Full HD", 1920, 1080),
                Arguments.of("SXGA", "SXGA", 1280, 1024),
                Arguments.of("HD", "HD", 1280, 720),
                Arguments.of("SD", "SD", 640, 480)
        );
    }
}
