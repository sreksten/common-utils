package com.threeamigos.common.util.implementations.messagehandler;

import com.threeamigos.common.util.interfaces.messagehandler.MessageHandler;
import jakarta.annotation.Nonnull;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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

    private static volatile ResourceBundle bundle;

    private static ResourceBundle getBundle() {
        ResourceBundle local = bundle;
        if (local == null) {
            synchronized (CompositeMessageHandler.class) {
                local = bundle;
                if (local == null) {
                    local = ResourceBundle.getBundle("com.threeamigos.common.util.implementations.messagehandler.CompositeMessageHandler.CompositeMessageHandler");
                    bundle = local;
                }
            }
        }
        return local;
    }

    // End of static methods

    private final List<MessageHandler> messageHandlers = new ArrayList<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

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
        lock.writeLock().lock();
        try {
            for (MessageHandler handler : handlers) {
                if (handler == null) {
                    throw new IllegalArgumentException(getBundle().getString("nullMessageHandlerProvided"));
                } else {
                    messageHandlers.add(handler);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * @param messageHandler a non-null MessageHandler to add
     */
    public void addMessageHandler(final @Nonnull MessageHandler messageHandler) {
        if (messageHandler == null) {
            throw new IllegalArgumentException(getBundle().getString("nullMessageHandlerProvided"));
        }
        lock.writeLock().lock();
        try {
            messageHandlers.add(messageHandler);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * @param messageHandler a non-null MessageHandler to remove
     */
    public void removeMessageHandler(final @Nonnull MessageHandler messageHandler) {
        if (messageHandler == null) {
            throw new IllegalArgumentException(getBundle().getString("nullMessageHandlerProvided"));
        }
        lock.writeLock().lock();
        try {
            messageHandlers.remove(messageHandler);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * @return an unmodifiable collection of the registered MessageHandlers
     */
    public Collection<MessageHandler> getMessageHandlers() {
        lock.readLock().lock();
        try {
            return Collections.unmodifiableList(new ArrayList<>(messageHandlers));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    protected void handleInfoMessageImpl(final String message) {
        forEachHandler(mh -> mh.handleInfoMessage(message));
    }

    @Override
    protected void handleWarnMessageImpl(final String message) {
        forEachHandler(mh -> mh.handleWarnMessage(message));
    }

    @Override
    protected void handleErrorMessageImpl(final String message) {
        forEachHandler(mh -> mh.handleErrorMessage(message));
    }

    @Override
    protected void handleDebugMessageImpl(final String message) {
        forEachHandler(mh -> mh.handleDebugMessage(message));
    }

    @Override
    protected void handleTraceMessageImpl(final String message) {
        forEachHandler(mh -> mh.handleTraceMessage(message));
    }

    @Override
    protected void handleExceptionImpl(final Exception exception) {
        forEachHandler(mh -> mh.handleException(exception));
    }

    private void forEachHandler(java.util.function.Consumer<MessageHandler> consumer) {
        lock.readLock().lock();
        try {
            for (MessageHandler handler : messageHandlers) {
                consumer.accept(handler);
            }
        } finally {
            lock.readLock().unlock();
        }
    }
}
