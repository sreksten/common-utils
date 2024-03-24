package com.threeamigos.common.util.interfaces.messagehandler;

/**
 * An interface used to handle error messages.
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
    void handleErrorMessage(String errorMessage);

}
