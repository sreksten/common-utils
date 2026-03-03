package com.threeamigos.common.util.interfaces.messagehandler;

import jakarta.annotation.Nonnull;

@FunctionalInterface
public interface ExceptionWithMessageHandler {

    /**
     * Handles a single exception.
     *
     * @param exception an exception to handle
     */
    void handleException(final @Nonnull String message, final @Nonnull Exception exception);

}
