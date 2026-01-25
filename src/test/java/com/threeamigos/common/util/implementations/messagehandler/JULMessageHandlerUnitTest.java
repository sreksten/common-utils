package com.threeamigos.common.util.implementations.messagehandler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JULMessageHandler unit tests")
class JULMessageHandlerUnitTest {

    private static class CapturingHandler extends Handler {
        LogRecord last;
        @Override
        public void publish(LogRecord record) {
            last = record;
        }
        @Override
        public void flush() {}
        @Override
        public void close() throws SecurityException {}
    }

    @Test
    @DisplayName("Should throw on null logger name")
    void shouldThrowOnNullLoggerName() {
        assertThrows(IllegalArgumentException.class, () -> new JULMessageHandler((String) null));
        assertThrows(IllegalArgumentException.class, () -> new JULMessageHandler(" "));
    }

    @Test
    @DisplayName("Should throw on null Logger instance")
    void shouldThrowOnNullLoggerInstance() {
        assertThrows(NullPointerException.class, () -> new JULMessageHandler((java.util.logging.Logger) null));
    }

    @Test
    @DisplayName("Should create logger by name")
    void shouldCreateLoggerByName() {
        JULMessageHandler handler = new JULMessageHandler("jul-handler-name");
        assertNotNull(handler);
    }

    @Test
    @DisplayName("Should bridge to JUL levels")
    void shouldBridgeToJulLevels() {
        Logger logger = Logger.getLogger("test-jul-handler");
        logger.setUseParentHandlers(false);
        CapturingHandler capturingHandler = new CapturingHandler();
        logger.addHandler(capturingHandler);
        logger.setLevel(Level.FINER);

        JULMessageHandler handler = new JULMessageHandler(logger);
        handler.handleInfoMessage("info");
        assertEquals(Level.INFO, capturingHandler.last.getLevel());
        assertEquals("info", capturingHandler.last.getMessage());

        handler.handleWarnMessage("warn");
        assertEquals(Level.WARNING, capturingHandler.last.getLevel());

        handler.handleErrorMessage("error");
        assertEquals(Level.SEVERE, capturingHandler.last.getLevel());

        handler.handleDebugMessage("debug");
        assertEquals(Level.FINE, capturingHandler.last.getLevel());

        handler.handleTraceMessage("trace");
        assertEquals(Level.FINER, capturingHandler.last.getLevel());

        handler.handleException(new RuntimeException("boom"));
        assertEquals(Level.SEVERE, capturingHandler.last.getLevel());
        assertNotNull(capturingHandler.last.getThrown());
    }

    @Test
    @DisplayName("Should honor enabled flags")
    void shouldHonorEnabledFlags() {
        Logger logger = Logger.getLogger("test-jul-handler-flags");
        logger.setUseParentHandlers(false);
        CapturingHandler capturingHandler = new CapturingHandler();
        logger.addHandler(capturingHandler);
        logger.setLevel(Level.ALL);

        JULMessageHandler handler = new JULMessageHandler(logger);
        handler.setInfoEnabled(false);
        handler.setWarnEnabled(false);
        handler.setErrorEnabled(false);
        handler.setDebugEnabled(false);
        handler.setTraceEnabled(false);
        handler.setExceptionEnabled(false);

        handler.handleInfoMessage("info");
        handler.handleWarnMessage("warn");
        handler.handleErrorMessage("error");
        handler.handleDebugMessage("debug");
        handler.handleTraceMessage("trace");
        handler.handleException(new RuntimeException("boom"));

        assertNull(capturingHandler.last, "No messages should be published when disabled");
    }
}
