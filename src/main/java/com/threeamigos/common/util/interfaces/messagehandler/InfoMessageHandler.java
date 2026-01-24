package com.threeamigos.common.util.interfaces.messagehandler;

import jakarta.annotation.Nonnull;

/**
 * An interface used to handle information messages.<br/>
 * If the message construction may be expensive, consider using a supplier instead
 * (see {@link SupplierInfoMessageHandler}).
 *
 * @author Stefano Reksten
 */
@FunctionalInterface
public interface InfoMessageHandler {

    /**
     * Handles a single information message.
     *
     * @param infoMessage an info message to show to the user
     */
    void handleInfoMessage(final @Nonnull String infoMessage);

}
