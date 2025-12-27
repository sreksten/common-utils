package com.threeamigos.common.util.interfaces.messagehandler;

import org.jspecify.annotations.NonNull;

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
    void handleErrorMessage(@NonNull String errorMessage);

}
