package com.threeamigos.common.util.implementations.messagehandler;

import com.threeamigos.common.util.interfaces.messagehandler.MessageHandler;

/**
 * An implementation of the {@link MessageHandler} interface that uses the
 * console to print info and warning messages to System.out and messages and
 * exceptions to System.err.
 *
 * @author Stefano Reksten
 */
public class ConsoleMessageHandler implements MessageHandler {

    @Override
    public void handleInfoMessage(final String message) {
        System.out.println(message); //NOSONAR
    }

    @Override
    public void handleWarnMessage(final String message) {
        System.out.println(message); //NOSONAR
    }

    @Override
    public void handleErrorMessage(final String message) {
        System.err.println(message); //NOSONAR
    }

    @Override
    public void handleException(final Exception exception) {
        exception.printStackTrace(System.err); //NOSONAR
    }

}
