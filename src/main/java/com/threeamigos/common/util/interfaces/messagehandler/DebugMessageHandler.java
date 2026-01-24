package com.threeamigos.common.util.interfaces.messagehandler;

import jakarta.annotation.Nonnull;

/**
 * An interface used to handle debug messages.<br/>
 * If the message construction may be expensive, consider using a supplier instead
 * (see {@link SupplierDebugMessageHandler}).
 *
 * @author Stefano Reksten
 */
@FunctionalInterface
public interface DebugMessageHandler {

    /**
     * Handles a single debug message.
     *
     * @param debugMessage a debug message to show to the programmer
     */
    void handleDebugMessage(final @Nonnull String debugMessage);

}
