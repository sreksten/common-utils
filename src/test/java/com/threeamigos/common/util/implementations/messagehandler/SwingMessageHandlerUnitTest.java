package com.threeamigos.common.util.implementations.messagehandler;

import com.threeamigos.common.util.ui.AWTCalls;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import javax.swing.*;
import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("SwingMessageHandler unit test")
@Tag("unit")
@Tag("messageHandler")
class SwingMessageHandlerUnitTest {

    @Test
    @DisplayName("No-args constructor should not set a parent component")
    void noArgsConstructor() {
        // Given
        // When
        SwingMessageHandler swingMessageHandler = new SwingMessageHandler();
        // Then
        assertNull(swingMessageHandler.getParentComponent(), "No-args constructor should not set a component");
    }

    @Test
    @DisplayName("Parameterized constructor should set a parent component")
    void componentConstructor() {
        // Given
        Component component = new JLabel();
        // When
        SwingMessageHandler swingMessageHandler = new SwingMessageHandler(component);
        // Then
        assertEquals(component, swingMessageHandler.getParentComponent(), "Wrong parent component");
    }

    @Test
    @DisplayName("Component setter works")
    void componentSetterWorks() {
        // Given
        Component component = new JLabel();
        SwingMessageHandler swingMessageHandler = new SwingMessageHandler();
        // When
        swingMessageHandler.setParentComponent(component);
        // Then
        assertEquals(component, swingMessageHandler.getParentComponent(), "Wrong parent component");
    }

    @Test
    @DisplayName("Should throw an exception if a null info message is provided")
    void shouldThrowAnExceptionIfANullInfoMessageIsProvided() {
        // Given
        SwingMessageHandler sut = new SwingMessageHandler();
        // When
        String infoMessage = null;
        // Then
        assertThrows(IllegalArgumentException.class, () -> sut.handleInfoMessage(infoMessage));
    }

    @Test
    @DisplayName("Should throw an exception if a null warn message is provided")
    void shouldThrowAnExceptionIfANullWarnMessageIsProvided() {
        // Given
        SwingMessageHandler sut = new SwingMessageHandler();
        // When
        String warnMessage = null;
        // Then
        assertThrows(IllegalArgumentException.class, () -> sut.handleWarnMessage(warnMessage));
    }

    @Test
    @DisplayName("Should throw an exception if a null error message is provided")
    void shouldThrowAnExceptionIfANullErrorMessageIsProvided() {
        // Given
        SwingMessageHandler sut = new SwingMessageHandler();
        // When
        String errorMessage = null;
        // Then
        assertThrows(IllegalArgumentException.class, () -> sut.handleErrorMessage(errorMessage));
    }

    @Test
    @DisplayName("Should throw an exception if a null debug message is provided")
    void shouldThrowAnExceptionIfANullDebugMessageIsProvided() {
        // Given
        SwingMessageHandler sut = new SwingMessageHandler();
        // When
        String debugMessage = null;
        // Then
        assertThrows(IllegalArgumentException.class, () -> sut.handleDebugMessage(debugMessage));
    }

    @Test
    @DisplayName("Should throw an exception if a null trace message is provided")
    void shouldThrowAnExceptionIfANullTraceMessageIsProvided() {
        // Given
        SwingMessageHandler sut = new SwingMessageHandler();
        // When
        String traceMessage = null;
        // Then
        assertThrows(IllegalArgumentException.class, () -> sut.handleTraceMessage(traceMessage));
    }

    @Test
    @DisplayName("Should throw an exception if a null exception is provided")
    void shouldThrowAnExceptionIfANullExceptionIsProvided() {
        // Given
        SwingMessageHandler sut = new SwingMessageHandler();
        // When
        Exception exception = null;
        // Then
        assertThrows(IllegalArgumentException.class, () -> sut.handleException(exception));
    }

    @Nested
    @DisplayName("Display in headless mode")
    @DisabledIfEnvironmentVariable(named = "AWT_TESTS", matches = "true", disabledReason = "Environment variable AWT_TESTS is true")
    class DisplayHeadlessMode {

        private ArgumentCaptor<String> titleCaptor;
        private ArgumentCaptor<String> messageCaptor;
        private ArgumentCaptor<Integer> iconCaptor;

        @BeforeEach
        void setup() {
            messageCaptor = ArgumentCaptor.forClass(String.class);
            titleCaptor = ArgumentCaptor.forClass(String.class);
            iconCaptor = ArgumentCaptor.forClass(Integer.class);
        }

        @Test
        @DisplayName("Displays an Info message")
        void displaysInfoMessage() {
            // Given
            SwingMessageHandler swingMessageHandler = new SwingMessageHandler();
            try (MockedStatic<AWTCalls> mockAWTCalls = Mockito.mockStatic(AWTCalls.class)) {
                mockAWTCalls.when(() -> AWTCalls.showOptionPane(any(), anyString(), anyString(), anyInt())).thenAnswer((Answer<Void>) invocation -> null);
                // When
                swingMessageHandler.handleInfoMessage("My message");
                // Then
                mockAWTCalls.verify(() -> AWTCalls.showOptionPane(any(), messageCaptor.capture(), titleCaptor.capture(), iconCaptor.capture()), times(1));
                assertEquals("Info", titleCaptor.getValue());
                assertEquals("My message", messageCaptor.getValue());
                assertEquals(JOptionPane.INFORMATION_MESSAGE, iconCaptor.getValue());
            }
        }

        @Test
        @DisplayName("Displays a Warn message")
        void displaysWarnMessage() {
            // Given
            SwingMessageHandler swingMessageHandler = new SwingMessageHandler();
            try (MockedStatic<AWTCalls> mockAWTCalls = Mockito.mockStatic(AWTCalls.class)) {
                mockAWTCalls.when(() -> AWTCalls.showOptionPane(any(), anyString(), anyString(), anyInt())).thenAnswer((Answer<Void>) invocation -> null);
                // When
                swingMessageHandler.handleWarnMessage("My warning");
                // Then
                mockAWTCalls.verify(() -> AWTCalls.showOptionPane(any(), messageCaptor.capture(), titleCaptor.capture(), iconCaptor.capture()), times(1));
                assertEquals("Warning", titleCaptor.getValue());
                assertEquals("My warning", messageCaptor.getValue());
                assertEquals(JOptionPane.WARNING_MESSAGE, iconCaptor.getValue());
            }
        }

        @Test
        @DisplayName("Displays an Error message")
        void displaysErrorMessage() {
            // Given
            SwingMessageHandler swingMessageHandler = new SwingMessageHandler();
            try (MockedStatic<AWTCalls> mockAWTCalls = Mockito.mockStatic(AWTCalls.class)) {
                mockAWTCalls.when(() -> AWTCalls.showOptionPane(any(), anyString(), anyString(), anyInt())).thenAnswer((Answer<Void>) invocation -> null);
                // When
                swingMessageHandler.handleErrorMessage("My error");
                // Then
                mockAWTCalls.verify(() -> AWTCalls.showOptionPane(any(), messageCaptor.capture(), titleCaptor.capture(), iconCaptor.capture()), times(1));
                assertEquals("Error", titleCaptor.getValue());
                assertEquals("My error", messageCaptor.getValue());
                assertEquals(JOptionPane.ERROR_MESSAGE, iconCaptor.getValue());
            }
        }

        @Test
        @DisplayName("Displays an Exception")
        void displaysException() {
            // Given
            SwingMessageHandler swingMessageHandler = new SwingMessageHandler();
            Exception exception = new Exception("My exception");
            try (MockedStatic<AWTCalls> mockAWTCalls = Mockito.mockStatic(AWTCalls.class)) {
                mockAWTCalls.when(() -> AWTCalls.showOptionPane(any(), anyString(), anyString(), anyInt())).thenAnswer((Answer<Void>) invocation -> null);
                // When
                swingMessageHandler.handleException(exception);
                // Then
                mockAWTCalls.verify(() -> AWTCalls.showOptionPane(any(), messageCaptor.capture(), titleCaptor.capture(), iconCaptor.capture()), times(1));
                assertEquals("Exception", titleCaptor.getValue());
                assertEquals("My exception", messageCaptor.getValue());
                assertEquals(JOptionPane.ERROR_MESSAGE, iconCaptor.getValue());
            }
        }
    }

    @Nested
    @DisplayName("Display in graphic mode")
    @EnabledIfEnvironmentVariable(named = "AWT_TESTS", matches = "true", disabledReason = "Environment variable AWT_TESTS is not true")
    @Execution(ExecutionMode.SAME_THREAD)
    @ResourceLock(value = "java.lang.System#properties", mode = ResourceAccessMode.READ_WRITE)
    class DisplayGraphicsMode {

        private ArgumentCaptor<String> titleCaptor;
        private ArgumentCaptor<String> messageCaptor;
        private ArgumentCaptor<Integer> iconCaptor;

        @BeforeEach
        void setup() {
            messageCaptor = ArgumentCaptor.forClass(String.class);
            titleCaptor = ArgumentCaptor.forClass(String.class);
            iconCaptor = ArgumentCaptor.forClass(Integer.class);
        }

        @Test
        @DisplayName("Displays an Info message")
        void displaysInfoMessage() {
            // Given
            SwingMessageHandler swingMessageHandler = new SwingMessageHandler();
            try (MockedStatic<AWTCalls> mockAWTCalls = Mockito.mockStatic(AWTCalls.class)) {
                mockAWTCalls.when(() -> AWTCalls.showOptionPane(any(), anyString(), anyString(), anyInt())).thenCallRealMethod();
                // When
                swingMessageHandler.handleInfoMessage("My message");
                // Then
                mockAWTCalls.verify(() -> AWTCalls.showOptionPane(any(), messageCaptor.capture(), titleCaptor.capture(), iconCaptor.capture()), times(1));
                assertEquals("Info", titleCaptor.getValue());
                assertEquals("My message", messageCaptor.getValue());
                assertEquals(JOptionPane.INFORMATION_MESSAGE, iconCaptor.getValue());
            }
        }

        @Test
        @DisplayName("Displays a Warn message")
        void displaysWarnMessage() {
            // Given
            SwingMessageHandler swingMessageHandler = new SwingMessageHandler();
            try (MockedStatic<AWTCalls> mockAWTCalls = Mockito.mockStatic(AWTCalls.class)) {
                mockAWTCalls.when(() -> AWTCalls.showOptionPane(any(), anyString(), anyString(), anyInt())).thenCallRealMethod();
                // When
                swingMessageHandler.handleWarnMessage("My warning");
                // Then
                mockAWTCalls.verify(() -> AWTCalls.showOptionPane(any(), messageCaptor.capture(), titleCaptor.capture(), iconCaptor.capture()), times(1));
                assertEquals("Warning", titleCaptor.getValue());
                assertEquals("My warning", messageCaptor.getValue());
                assertEquals(JOptionPane.WARNING_MESSAGE, iconCaptor.getValue());
            }
        }

        @Test
        @DisplayName("Displays an Error message")
        void displaysErrorMessage() {
            // Given
            SwingMessageHandler swingMessageHandler = new SwingMessageHandler();
            try (MockedStatic<AWTCalls> mockAWTCalls = Mockito.mockStatic(AWTCalls.class)) {
                mockAWTCalls.when(() -> AWTCalls.showOptionPane(any(), anyString(), anyString(), anyInt())).thenCallRealMethod();
                // When
                swingMessageHandler.handleErrorMessage("My error");
                // Then
                mockAWTCalls.verify(() -> AWTCalls.showOptionPane(any(), messageCaptor.capture(), titleCaptor.capture(), iconCaptor.capture()), times(1));
                assertEquals("Error", titleCaptor.getValue());
                assertEquals("My error", messageCaptor.getValue());
                assertEquals(JOptionPane.ERROR_MESSAGE, iconCaptor.getValue());
            }
        }

        @Test
        @DisplayName("Displays a Debug message")
        void displaysDebugMessage() {
            // Given
            SwingMessageHandler swingMessageHandler = new SwingMessageHandler();
            try (MockedStatic<AWTCalls> mockAWTCalls = Mockito.mockStatic(AWTCalls.class)) {
                mockAWTCalls.when(() -> AWTCalls.showOptionPane(any(), anyString(), anyString(), anyInt())).thenCallRealMethod();
                // When
                swingMessageHandler.handleDebugMessage("My debug");
                // Then
                mockAWTCalls.verify(() -> AWTCalls.showOptionPane(any(), messageCaptor.capture(), titleCaptor.capture(), iconCaptor.capture()), times(1));
                assertEquals("Debug", titleCaptor.getValue());
                assertEquals("My debug", messageCaptor.getValue());
                assertEquals(JOptionPane.INFORMATION_MESSAGE, iconCaptor.getValue());
            }
        }

        @Test
        @DisplayName("Displays a Trace message")
        void displaysTraceMessage() {
            // Given
            SwingMessageHandler swingMessageHandler = new SwingMessageHandler();
            try (MockedStatic<AWTCalls> mockAWTCalls = Mockito.mockStatic(AWTCalls.class)) {
                mockAWTCalls.when(() -> AWTCalls.showOptionPane(any(), anyString(), anyString(), anyInt())).thenCallRealMethod();
                // When
                swingMessageHandler.handleTraceMessage("My trace");
                // Then
                mockAWTCalls.verify(() -> AWTCalls.showOptionPane(any(), messageCaptor.capture(), titleCaptor.capture(), iconCaptor.capture()), times(1));
                assertEquals("Trace", titleCaptor.getValue());
                assertEquals("My trace", messageCaptor.getValue());
                assertEquals(JOptionPane.INFORMATION_MESSAGE, iconCaptor.getValue());
            }
        }

        @Test
        @DisplayName("Displays an Exception")
        void displaysException() {
            // Given
            SwingMessageHandler swingMessageHandler = new SwingMessageHandler();
            Exception exception = new Exception("My exception");
            try (MockedStatic<AWTCalls> mockAWTCalls = Mockito.mockStatic(AWTCalls.class)) {
                mockAWTCalls.when(() -> AWTCalls.showOptionPane(any(), anyString(), anyString(), anyInt())).thenCallRealMethod();
                // When
                swingMessageHandler.handleException(exception);
                // Then
                mockAWTCalls.verify(() -> AWTCalls.showOptionPane(any(), messageCaptor.capture(), titleCaptor.capture(), iconCaptor.capture()), times(1));
                assertEquals("Exception", titleCaptor.getValue());
                assertEquals("My exception", messageCaptor.getValue());
                assertEquals(JOptionPane.ERROR_MESSAGE, iconCaptor.getValue());
            }
        }
    }
}