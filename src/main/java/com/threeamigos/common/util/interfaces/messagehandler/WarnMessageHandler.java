package com.threeamigos.common.util.interfaces.messagehandler;

import jakarta.annotation.Nonnull;

/**
 * An interface used to handle warning messages.<br/>
 * If the message construction may be expensive, consider using a supplier instead
 * (see {@link SupplierWarnMessageHandler}).
 *
 * @author Stefano Reksten
 */
@FunctionalInterface
public interface WarnMessageHandler {

    /**
     * Handles a single warning message.
     *
     * @param message a warning message to show to the user
     */
    void handleWarnMessage(final @Nonnull String message);

}
