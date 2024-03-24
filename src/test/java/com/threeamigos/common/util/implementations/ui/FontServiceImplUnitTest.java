package com.threeamigos.common.util.implementations.ui;

import com.threeamigos.common.util.interfaces.ui.FontService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;

import java.awt.*;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("FontServiceImpl unit test")
@Tag("unit")
@DisabledIf(value = "java.awt.GraphicsEnvironment#isHeadless", disabledReason = "Headless environment")
class FontServiceImplUnitTest {

    @Test
    @DisplayName("Should return a font")
    void shouldReturnFont() {
        // Given
        FontService fontService = new FontServiceImpl();
        // When
        Font font = fontService.getFont("Arial", Font.PLAIN, 16);
        // Then
        assertNotNull(font);
    }

}