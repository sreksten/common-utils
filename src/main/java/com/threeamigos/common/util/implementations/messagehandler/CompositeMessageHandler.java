package com.threeamigos.common.util.implementations.messagehandler;

import com.threeamigos.common.util.interfaces.messagehandler.MessageHandler;
import org.jspecify.annotations.NonNull;

import java.util.*;

/**
 * An implementation of the {@link MessageHandler} interface that forwards
 * messages and exceptions to one or more other handlers.
 *
 * @author Stefano Reksten
 */
public class CompositeMessageHandler implements MessageHandler {

    private static ResourceBundle bundle;

    private static ResourceBundle getBundle() {
        if (bundle == null) {
            bundle = ResourceBundle.getBundle("com.threeamigos.common.util.implementations.messagehandler.CompositeMessageHandler.CompositeMessageHandler");
        }
        return bundle;
    }

    private final List<MessageHandler> messageHandlers = new ArrayList<>();

    public CompositeMessageHandler(final MessageHandler... handlers) {
        if (handlers == null) {
            throw new IllegalArgumentException(getBundle().getString("noMessageHandlersProvided"));
        }
        for (MessageHandler handler : handlers) {
            if (handler == null) {
                throw new IllegalArgumentException(getBundle().getString("nullMessageHandlerProvided"));
            } else {
                messageHandlers.add(handler);
            }
        }
    }

    public void addMessageHandler(final @NonNull MessageHandler messageHandler) {
        if (messageHandler == null) {
            throw new IllegalArgumentException(getBundle().getString("nullMessageHandlerProvided"));
        }
        messageHandlers.add(messageHandler);
    }

    public void removeMessageHandler(final @NonNull MessageHandler messageHandler) {
        if (messageHandler == null) {
            throw new IllegalArgumentException(getBundle().getString("nullMessageHandlerProvided"));
        }
        messageHandlers.remove(messageHandler);
    }

    public Collection<MessageHandler> getMessageHandlers() {
        return Collections.unmodifiableList(messageHandlers);
    }

    @Override
    public void handleInfoMessage(final @NonNull String message) {
        if (message == null) {
            throw new IllegalArgumentException(getBundle().getString("nullMessageProvided"));
        }
        messageHandlers.forEach(mh -> mh.handleInfoMessage(message));
    }

    @Override
    public void handleWarnMessage(final @NonNull String message) {
        if (message == null) {
            throw new IllegalArgumentException(getBundle().getString("nullMessageProvided"));
        }
        messageHandlers.forEach(mh -> mh.handleWarnMessage(message));
    }

    @Override
    public void handleErrorMessage(final @NonNull String message) {
        if (message == null) {
            throw new IllegalArgumentException(getBundle().getString("nullMessageProvided"));
        }
        messageHandlers.forEach(mh -> mh.handleErrorMessage(message));
    }

    @Override
    public void handleDebugMessage(final @NonNull String message) {
        if (message == null) {
            throw new IllegalArgumentException(getBundle().getString("nullMessageProvided"));
        }
        messageHandlers.forEach(mh -> mh.handleDebugMessage(message));
    }

    @Override
    public void handleTraceMessage(final @NonNull String message) {
        if (message == null) {
            throw new IllegalArgumentException(getBundle().getString("nullMessageProvided"));
        }
        messageHandlers.forEach(mh -> mh.handleTraceMessage(message));
    }

    @Override
    public void handleException(final @NonNull Exception exception) {
        if (exception == null) {
            throw new IllegalArgumentException(getBundle().getString("nullExceptionProvided"));
        }
        messageHandlers.forEach(mh -> mh.handleException(exception));
    }

}
