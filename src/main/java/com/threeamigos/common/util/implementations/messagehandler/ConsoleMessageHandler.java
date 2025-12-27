package com.threeamigos.common.util.implementations.messagehandler;

import com.threeamigos.common.util.interfaces.messagehandler.MessageHandler;
import org.jspecify.annotations.NonNull;

import java.util.ResourceBundle;

/**
 * An implementation of the {@link MessageHandler} interface that uses the
 * console to print info and warning messages to System.out and messages and
 * exceptions to System.err.
 *
 * @author Stefano Reksten
 */
public class ConsoleMessageHandler implements MessageHandler {

    private static ResourceBundle bundle;

    private static ResourceBundle getBundle() {
        if (bundle == null) {
            bundle = ResourceBundle.getBundle("com.threeamigos.common.util.implementations.messagehandler.ConsoleMessageHandler.ConsoleMessageHandler");
        }
        return bundle;
    }

    @Override
    public void handleInfoMessage(final @NonNull String message) {
        if (message == null) {
            throw new IllegalArgumentException(getBundle().getString("nullMessageProvided"));
        }
        System.out.println(message); //NOSONAR
    }

    @Override
    public void handleWarnMessage(final @NonNull String message) {
        if (message == null) {
            throw new IllegalArgumentException(getBundle().getString("nullMessageProvided"));
        }
        System.out.println(message); //NOSONAR
    }

    @Override
    public void handleErrorMessage(final @NonNull String message) {
        if (message == null) {
            throw new IllegalArgumentException(getBundle().getString("nullMessageProvided"));
        }
        System.err.println(message); //NOSONAR
    }

    @Override
    public void handleDebugMessage(final @NonNull String message) {
        if (message == null) {
            throw new IllegalArgumentException(getBundle().getString("nullMessageProvided"));
        }
        System.out.println(message); //NOSONAR
    }

    @Override
    public void handleTraceMessage(final @NonNull String message) {
        if (message == null) {
            throw new IllegalArgumentException(getBundle().getString("nullMessageProvided"));
        }
        System.out.println(message); //NOSONAR
    }

    @Override
    public void handleException(final @NonNull Exception exception) {
        if (exception == null) {
            throw new IllegalArgumentException(getBundle().getString("nullExceptionProvided"));
        }
        exception.printStackTrace(System.err); //NOSONAR
    }

}
