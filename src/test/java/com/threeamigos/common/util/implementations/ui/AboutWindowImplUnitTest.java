package com.threeamigos.common.util.implementations.ui;

import com.threeamigos.common.util.interfaces.ui.AboutWindow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AboutWindowImpl unit test")
@Tag("unit")
@EnabledIfEnvironmentVariable(named = "AWT_TESTS", matches = "true", disabledReason = "AWT_TESTS is not true")
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock(value = "java.lang.System#properties", mode = ResourceAccessMode.READ_WRITE)
class AboutWindowImplUnitTest {

    @Test
    @DisplayName("Should display standard about window")
    void shouldDisplayStandardAboutWindow() {
        // Given
        AboutWindow sut = new AboutWindowImpl("3AM Test Application",
                "by Stefano Reksten - stefano.reksten@gmail.com",
                "Released under the 3AM license");
        // When
        sut.about(null);
        // Then it should simply work
        assertTrue(true);
    }

    @Test
    @DisplayName("Should display about window with custom image")
    void shouldDisplaydAboutWindowWithCustomImage() {
        // Given
        AboutWindow sut = new AboutWindowImpl("3AM Test Application with custom image",
                "by Stefano Reksten - stefano.reksten@gmail.com",
                "Released under the 3AM license",
                getClass().getResource("/custom_logo.png"));
        // When
        sut.about(null);
        // Then it should simply work
        assertTrue(true);
    }


}