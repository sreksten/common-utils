package com.threeamigos.common.util.implementations.messagehandler;

import com.threeamigos.common.util.interfaces.messagehandler.MessageHandler;

import java.util.*;

/**
 * An implementation of the {@link MessageHandler} interface that forwards
 * messages and exceptions to one or more other handlers.
 *
 * @author Stefano Reksten
 */
public class CompositeMessageHandler implements MessageHandler {

    private final List<MessageHandler> messageHandlers = new ArrayList<>();

    public CompositeMessageHandler(final MessageHandler... handlers) {
        messageHandlers.addAll(Arrays.asList(handlers));
    }

    public void addMessageHandler(final MessageHandler messageHandler) {
        messageHandlers.add(messageHandler);
    }

    public void removeMessageHandler(final MessageHandler messageHandler) {
        messageHandlers.remove(messageHandler);
    }

    public Collection<MessageHandler> getMessageHandlers() {
        return Collections.unmodifiableList(messageHandlers);
    }

    @Override
    public void handleInfoMessage(final String message) {
        messageHandlers.forEach(mh -> mh.handleInfoMessage(message));
    }

    @Override
    public void handleWarnMessage(final String message) {
        messageHandlers.forEach(mh -> mh.handleWarnMessage(message));
    }

    @Override
    public void handleErrorMessage(final String message) {
        messageHandlers.forEach(mh -> mh.handleErrorMessage(message));
    }

    @Override
    public void handleException(final Exception exception) {
        messageHandlers.forEach(mh -> mh.handleException(exception));
    }

}
