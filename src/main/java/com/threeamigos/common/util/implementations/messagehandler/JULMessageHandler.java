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
    protected void handleInfoMessageImpl(final String message) {
        logger.log(Level.INFO, message);
    }

    @Override
    protected void handleWarnMessageImpl(final String message) {
        logger.log(Level.WARNING, message);
    }

    @Override
    protected void handleErrorMessageImpl(final String message) {
        logger.log(Level.SEVERE, message);
    }

    @Override
    protected void handleDebugMessageImpl(final String message) {
        logger.log(Level.FINE, message);
    }

    @Override
    protected void handleTraceMessageImpl(final String message) {
        logger.log(Level.FINER, message);
    }

    @Override
    protected void handleExceptionImpl(final Exception exception) {
        logger.log(Level.SEVERE, exception.getMessage(), exception);
    }

    @Override
    protected void handleExceptionImpl(final String message, final Exception exception) {
        logger.log(Level.SEVERE, message + ": " + exception.getMessage(), exception);
    }
}
