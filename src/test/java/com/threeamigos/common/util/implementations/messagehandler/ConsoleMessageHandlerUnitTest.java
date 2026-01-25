package com.threeamigos.common.util.implementations.messagehandler;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentCaptor.forClass;
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
    @DisplayName("Should throw an exception if a null info message is provided")
    void shouldThrowAnExceptionIfANullInfoMessageIsProvided() {
        // Given
        ConsoleMessageHandler sut = new ConsoleMessageHandler();
        // When
        String infoMessage = null;
        // Then
        assertThrows(IllegalArgumentException.class, () -> sut.handleInfoMessage(infoMessage));
    }

    @Test
    @DisplayName("Should handle info message")
    void shouldHandleInfoMessage() {
        // Given
        ConsoleMessageHandler sut = new ConsoleMessageHandler();
        // When
        sut.handleInfoMessage("INFO");
        // Then
        assertFormattedLine(out, "INFO ", "INFO");
    }

    @Test
    @DisplayName("Should throw an exception if a null warn message is provided")
    void shouldThrowAnExceptionIfANullWarnMessageIsProvided() {
        // Given
        ConsoleMessageHandler sut = new ConsoleMessageHandler();
        // When
        String warnMessage = null;
        // Then
        assertThrows(IllegalArgumentException.class, () -> sut.handleWarnMessage(warnMessage));
    }

    @Test
    @DisplayName("Should handle warn message")
    void shouldHandleWarnMessage() {
        // Given
        ConsoleMessageHandler sut = new ConsoleMessageHandler();
        // When
        sut.handleWarnMessage("WARN");
        // Then
        assertFormattedLine(out, "WARN ", "WARN");
    }

    @Test
    @DisplayName("Should throw an exception if a null error message is provided")
    void shouldThrowAnExceptionIfANullErrorMessageIsProvided() {
        // Given
        ConsoleMessageHandler sut = new ConsoleMessageHandler();
        // When
        String errorMessage = null;
        // Then
        assertThrows(IllegalArgumentException.class, () -> sut.handleErrorMessage(errorMessage));
    }

    @Test
    @DisplayName("Should handle error message")
    void shouldHandleErrorMessage() {
        // Given
        ConsoleMessageHandler sut = new ConsoleMessageHandler();
        // When
        sut.handleErrorMessage("ERROR");
        // Then
        assertFormattedLine(err, "ERROR", "ERROR");
    }

    @Test
    @DisplayName("Should throw an exception if a null debug message is provided")
    void shouldThrowAnExceptionIfANullDebugMessageIsProvided() {
        // Given
        ConsoleMessageHandler sut = new ConsoleMessageHandler();
        // When
        String debugMessage = null;
        // Then
        assertThrows(IllegalArgumentException.class, () -> sut.handleDebugMessage(debugMessage));
    }

    @Test
    @DisplayName("Should handle debug message")
    void shouldHandleDebugMessage() {
        // Given
        ConsoleMessageHandler sut = new ConsoleMessageHandler();
        // When
        sut.handleDebugMessage("DEBUG");
        // Then
        assertFormattedLine(out, "DEBUG", "DEBUG");
    }

    @Test
    @DisplayName("Should throw an exception if a null trace message is provided")
    void shouldThrowAnExceptionIfANullTraceMessageIsProvided() {
        // Given
        ConsoleMessageHandler sut = new ConsoleMessageHandler();
        // When
        String traceMessage = null;
        // Then
        assertThrows(IllegalArgumentException.class, () -> sut.handleTraceMessage(traceMessage));
    }

    @Test
    @DisplayName("Should handle trace message")
    void shouldHandleTraceMessage() {
        // Given
        ConsoleMessageHandler sut = new ConsoleMessageHandler();
        // When
        sut.handleTraceMessage("TRACE");
        // Then
        assertFormattedLine(out, "TRACE", "TRACE");
    }

    @Test
    @DisplayName("Should throw an exception if a null exception is provided")
    void shouldThrowAnExceptionIfANullExceptionIsProvided() {
        // Given
        ConsoleMessageHandler sut = new ConsoleMessageHandler();
        // When
        Exception exception = null;
        // Then
        assertThrows(IllegalArgumentException.class, () -> sut.handleException(exception));
    }

    @Test
    @DisplayName("Should handle exception")
    void shouldHandleExceptionMessage() {
        // Given
        ConsoleMessageHandler sut = new ConsoleMessageHandler();
        Exception exception = mock(IllegalArgumentException.class);
        when(exception.getMessage()).thenReturn("Boom");
        // When
        sut.handleException(exception);
        // Then
        assertFormattedLine(err, "EXCEP", "Boom");
        verify(exception, times(1)).printStackTrace(err);
    }

    @Test
    @DisplayName("Should run synchronously when queue is full")
    void shouldRunSynchronouslyWhenQueueIsFull() throws Exception {
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));
        try (ConsoleMessageHandler handler = new ConsoleMessageHandler(true, 1)) {
            CountDownLatch latch = new CountDownLatch(1);
            // Enqueue one long-running task to fill the queue
            handler.handleInfoMessage(() -> {
                try {
                    latch.await(500, TimeUnit.MILLISECONDS);
                } catch (InterruptedException ignored) {
                }
                return "background";
            });
            // This call should execute synchronously because queue is full
            handler.handleInfoMessage("sync");
            latch.countDown();
            // Wait for async worker to finish
            Thread.sleep(200);
            String output = outContent.toString();
            assertTrue(output.contains("sync"));
            assertTrue(output.contains("background"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    @DisplayName("Should register shutdown hook when requested")
    void shouldRegisterShutdownHook() throws Exception {
        ConsoleMessageHandler handler = new ConsoleMessageHandler(true, 1, true);
        Thread hook = getShutdownHook(handler);
        assertNotNull(hook);
        handler.close();
    }

    @Test
    @DisplayName("Should not register shutdown hook when not requested")
    void shouldNotRegisterShutdownHook() throws Exception {
        ConsoleMessageHandler handler = new ConsoleMessageHandler(true, 1, false);
        Thread hook = getShutdownHook(handler);
        assertNull(hook);
        handler.close();
    }

    private Thread getShutdownHook(ConsoleMessageHandler handler) throws Exception {
        Field f = ConsoleMessageHandler.class.getDeclaredField("shutdownHook");
        f.setAccessible(true);
        return (Thread) f.get(handler);
    }

    private void assertFormattedLine(PrintStream stream, String level, String message) {
        ArgumentCaptor<String> captor = forClass(String.class);
        verify(stream, times(1)).println(captor.capture());
        String actual = captor.getValue();
        String regex = "^\\[[^\\]]+\\] \\[" + Pattern.quote(level) + "\\] " + Pattern.quote(message) + "$";
        assertTrue(actual.matches(regex), () -> "Unexpected message: " + actual);
    }

}
