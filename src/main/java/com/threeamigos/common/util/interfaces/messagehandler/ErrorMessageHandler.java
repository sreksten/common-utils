package com.threeamigos.common.util.interfaces.messagehandler;

import jakarta.annotation.Nonnull;

/**
 * An interface used to handle error messages.<br/>
 * If the message construction may be expensive, consider using a supplier instead
 * (see {@link SupplierErrorMessageHandler}).
 *
 * @author Stefano Reksten
 */
@FunctionalInterface
public interface ErrorMessageHandler {

    /**
     * Handles a single error message.
     *
     * @param errorMessage an error message to show to the user
     */
    void handleErrorMessage(final @Nonnull String errorMessage);

}
