package com.threeamigos.common.util.interfaces.messagehandler;

/**
 * An interface used to handle information messages.
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
    void handleInfoMessage(String infoMessage);

}
