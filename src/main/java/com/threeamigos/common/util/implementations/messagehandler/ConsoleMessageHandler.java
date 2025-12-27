package com.threeamigos.common.util.implementations.messagehandler;

import com.threeamigos.common.util.interfaces.messagehandler.MessageHandler;
import org.jspecify.annotations.NonNull;

/**
 * An implementation of the {@link MessageHandler} interface that uses the
 * console to print info, warning, trace, and debug messages to System.out and messages and
 * exceptions to System.err.
 *
 * @author Stefano Reksten
 */
public class ConsoleMessageHandler extends AbstractMessageHandler {

    @Override
    protected void handleInfoMessageImpl(final @NonNull String message) {
        System.out.println(message); //NOSONAR
    }

    @Override
    protected void handleWarnMessageImpl(final @NonNull String message) {
        System.out.println(message); //NOSONAR
    }

    @Override
    protected void handleErrorMessageImpl(final @NonNull String message) {
        System.err.println(message); //NOSONAR
    }

    @Override
    protected void handleDebugMessageImpl(final @NonNull String message) {
        System.out.println(message); //NOSONAR
    }

    @Override
    protected void handleTraceMessageImpl(final @NonNull String message) {
        System.out.println(message); //NOSONAR
    }

    @Override
    protected void handleExceptionImpl(final @NonNull Exception exception) {
        exception.printStackTrace(System.err); //NOSONAR
    }
}
