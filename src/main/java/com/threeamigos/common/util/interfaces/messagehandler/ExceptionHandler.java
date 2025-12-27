package com.threeamigos.common.util.interfaces.messagehandler;

import org.jspecify.annotations.NonNull;

/**
 * An interface used to handle exceptions.
 *
 * @author Stefano Reksten
 */
@FunctionalInterface
public interface ExceptionHandler {

    /**
     * Handles a single exception.
     *
     * @param exception an exception to handle
     */
    void handleException(@NonNull Exception exception);

}
