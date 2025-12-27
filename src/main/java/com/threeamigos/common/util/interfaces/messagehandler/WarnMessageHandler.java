package com.threeamigos.common.util.interfaces.messagehandler;

import org.jspecify.annotations.NonNull;

/**
 * An interface used to handle warning messages.
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
    void handleWarnMessage(@NonNull String message);

}
