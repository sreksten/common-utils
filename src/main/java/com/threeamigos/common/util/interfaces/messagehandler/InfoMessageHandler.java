package com.threeamigos.common.util.interfaces.messagehandler;

import org.jspecify.annotations.NonNull;

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
    void handleInfoMessage(@NonNull String infoMessage);

}
