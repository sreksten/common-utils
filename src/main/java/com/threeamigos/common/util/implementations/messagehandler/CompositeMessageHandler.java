package com.threeamigos.common.util.implementations.messagehandler;

import com.threeamigos.common.util.interfaces.messagehandler.MessageHandler;
import jakarta.annotation.Nonnull;

import java.util.*;

/**
 * An implementation of the {@link MessageHandler} interface that forwards
 * messages and exceptions to one or more other MessageHandlers. Note that if you disable a certain message level from
 * here, no messages will be forwarded to any MessageHandler registered. The best option is to disable levels in the
 * handlers. In this way you can, for example, forward only error messages to a file, and all other messages to the
 * console.
 *
 * @author Stefano Reksten
 */
public class CompositeMessageHandler extends AbstractMessageHandler {

    private static ResourceBundle bundle;

    private static ResourceBundle getBundle() {
        if (bundle == null) {
            bundle = ResourceBundle.getBundle("com.threeamigos.common.util.implementations.messagehandler.CompositeMessageHandler.CompositeMessageHandler");
        }
        return bundle;
    }

    // End of static methods

    private final List<MessageHandler> messageHandlers = new ArrayList<>();

    /**
     * Constructor.
     * @param handlers a collection of non-null MessageHandlers
     */
    public CompositeMessageHandler(final @Nonnull Collection<MessageHandler> handlers) {
        if (handlers == null) {
            throw new IllegalArgumentException(getBundle().getString("noMessageHandlersProvided"));
        }
        addMessageHandlers(handlers);
    }

    /**
     * Constructor.
     * @param handlers a collection of non-null MessageHandlers
     */
    public CompositeMessageHandler(final @Nonnull MessageHandler... handlers) {
        if (handlers == null) {
            throw new IllegalArgumentException(getBundle().getString("noMessageHandlersProvided"));
        }
        addMessageHandlers(Arrays.asList(handlers));
    }

    private void addMessageHandlers(@Nonnull Collection<MessageHandler> handlers) {
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

    /**
     * @param messageHandler a non-null MessageHandler to add
     */
    public void addMessageHandler(final @Nonnull MessageHandler messageHandler) {
        if (messageHandler == null) {
            throw new IllegalArgumentException(getBundle().getString("nullMessageHandlerProvided"));
        }
        messageHandlers.add(messageHandler);
    }

    /**
     * @param messageHandler a non-null MessageHandler to remove
     */
    public void removeMessageHandler(final @Nonnull MessageHandler messageHandler) {
        if (messageHandler == null) {
            throw new IllegalArgumentException(getBundle().getString("nullMessageHandlerProvided"));
        }
        messageHandlers.remove(messageHandler);
    }

    /**
     * @return an unmodifiable collection of the registered MessageHandlers
     */
    public Collection<MessageHandler> getMessageHandlers() {
        return Collections.unmodifiableList(messageHandlers);
    }

    @Override
    protected void handleInfoMessageImpl(final String message) {
        messageHandlers.forEach(mh -> mh.handleInfoMessage(message));
    }

    @Override
    protected void handleWarnMessageImpl(final String message) {
        messageHandlers.forEach(mh -> mh.handleWarnMessage(message));
    }

    @Override
    protected void handleErrorMessageImpl(final String message) {
        messageHandlers.forEach(mh -> mh.handleErrorMessage(message));
    }

    @Override
    protected void handleDebugMessageImpl(final String message) {
        messageHandlers.forEach(mh -> mh.handleDebugMessage(message));
    }

    @Override
    protected void handleTraceMessageImpl(final String message) {
        messageHandlers.forEach(mh -> mh.handleTraceMessage(message));
    }

    @Override
    protected void handleExceptionImpl(final Exception exception) {
        messageHandlers.forEach(mh -> mh.handleException(exception));
    }
}
