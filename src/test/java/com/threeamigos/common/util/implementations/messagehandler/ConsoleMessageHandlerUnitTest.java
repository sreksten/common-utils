package com.threeamigos.common.util.implementations.messagehandler;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.PrintStream;

import static org.mockito.Mockito.*;

@DisplayName("ConsoleMessageHandler unit test")
@Tag("unit")
@Tag("messageHandler")
@Execution(ExecutionMode.SAME_THREAD)
class ConsoleMessageHandlerUnitTest {

    private static PrintStream systemOut;
    private static PrintStream systemErr;

    private PrintStream out;
    private PrintStream err;

    @BeforeAll
    static void setupBeforeAll() {
        systemOut = System.out;
        systemErr = System.err;
    }

    @BeforeEach
    void setup() {
        out = mock(PrintStream.class);
        System.setOut(out);
        err = mock(PrintStream.class);
        System.setErr(err);
    }

    @AfterAll
    static void cleanup() {
        System.setOut(systemOut);
        System.setErr(systemErr);
    }

    @Test
    @DisplayName("Should handle info message")
    void shouldHandleInfoMessage() {
        // Given
        ConsoleMessageHandler sut = new ConsoleMessageHandler();
        // When
        sut.handleInfoMessage("INFO");
        // Then
        verify(out, times(1)).println("INFO");
    }

    @Test
    @DisplayName("Should handle warn message")
    void shouldHandleWarnMessage() {
        // Given
        ConsoleMessageHandler sut = new ConsoleMessageHandler();
        // When
        sut.handleWarnMessage("WARN");
        // Then
        verify(out, times(1)).println("WARN");
    }

    @Test
    @DisplayName("Should handle error message")
    void shouldHandleErrorMessage() {
        // Given
        ConsoleMessageHandler sut = new ConsoleMessageHandler();
        // When
        sut.handleErrorMessage("ERROR");
        // Then
        verify(err, times(1)).println("ERROR");
    }

    @Test
    @DisplayName("Should handle exception")
    void shouldHandleExceptionMessage() {
        // Given
        ConsoleMessageHandler sut = new ConsoleMessageHandler();
        Exception exception = mock(IllegalArgumentException.class);
        // When
        sut.handleException(exception);
        // Then
        verify(exception, times(1)).printStackTrace(err);
    }

}