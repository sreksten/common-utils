package com.threeamigos.common.util.implementations.messagehandler;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bridge MessageHandler to java.util.logging.
 */
public class JULMessageHandler extends AbstractMessageHandler {

    private final Logger logger;

    public JULMessageHandler(Logger logger) {
        this.logger = Objects.requireNonNull(logger, "Logger cannot be null");
    }

    public JULMessageHandler(String loggerName) {
        if (loggerName == null || loggerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Logger name cannot be null or empty");
        }
        this.logger = Logger.getLogger(loggerName);
    }

    @Override
    protected void handleInfoMessageImpl(String message) {
        logger.log(Level.INFO, message);
    }

    @Override
    protected void handleWarnMessageImpl(String message) {
        logger.log(Level.WARNING, message);
    }

    @Override
    protected void handleErrorMessageImpl(String message) {
        logger.log(Level.SEVERE, message);
    }

    @Override
    protected void handleDebugMessageImpl(String message) {
        logger.log(Level.FINE, message);
    }

    @Override
    protected void handleTraceMessageImpl(String message) {
        logger.log(Level.FINER, message);
    }

    @Override
    protected void handleExceptionImpl(Exception exception) {
        logger.log(Level.SEVERE, exception.getMessage(), exception);
    }
}
