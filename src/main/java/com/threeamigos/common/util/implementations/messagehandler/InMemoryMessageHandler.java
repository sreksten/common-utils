package com.threeamigos.common.util.implementations.messagehandler;

import com.threeamigos.common.util.interfaces.messagehandler.MessageHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * An implementation of the {@link MessageHandler} interface that stores
 * messages and exceptions to an internal list for later processing (e.g.,
 * to conduct unit testing).
 *
 * @author Stefano Reksten
 */
public class InMemoryMessageHandler extends AbstractMessageHandler {

    private final int maxEntries;

    private final List<String> allMessages = new ArrayList<>();
    private final List<String> allInfoMessages = new ArrayList<>();
    private final List<String> allWarnMessages = new ArrayList<>();
    private final List<String> allErrorMessages = new ArrayList<>();
    private final List<String> allDebugMessages = new ArrayList<>();
    private final List<String> allTraceMessages = new ArrayList<>();
    private final List<String> allExceptionMessages = new ArrayList<>();
    private final List<Exception> allExceptions = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();
    private String lastMessage;

    public InMemoryMessageHandler() {
        this(10_000);
    }

    public InMemoryMessageHandler(int maxEntries) {
        if (maxEntries <= 0) {
            throw new IllegalArgumentException("maxEntries must be positive");
        }
        this.maxEntries = maxEntries;
    }

    @Override
    protected void handleInfoMessageImpl(final String message) {
        lock.lock();
        try {
            addWithLimit(allInfoMessages, message);
            handleImpl(message);
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected void handleWarnMessageImpl(final String message) {
        lock.lock();
        try {
            addWithLimit(allWarnMessages, message);
            handleImpl(message);
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected void handleErrorMessageImpl(final String message) {
        lock.lock();
        try {
            addWithLimit(allErrorMessages, message);
            handleImpl(message);
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected void handleDebugMessageImpl(final String message) {
        lock.lock();
        try {
            addWithLimit(allDebugMessages, message);
            handleImpl(message);
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected void handleTraceMessageImpl(final String message) {
        lock.lock();
        try {
            addWithLimit(allTraceMessages, message);
            handleImpl(message);
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected void handleExceptionImpl(final Exception exception) {
        lock.lock();
        try {
            String msg = exception.getMessage();
            addWithLimit(allExceptionMessages, msg);
            addWithLimit(allExceptions, exception);
            handleImpl(msg);
        } finally {
            lock.unlock();
        }
    }

    private void handleImpl(final String message) {
        addWithLimit(allMessages, message);
        lastMessage = message;
    }

    private <E> void addWithLimit(List<E> list, E value) {
        if (list.size() >= maxEntries) {
            list.remove(0);
        }
        list.add(value);
    }

    /**
     * @return an unmodifiable list of all messages handled by this instance.
     */
    public List<String> getAllMessages() {
        lock.lock();
        try {
            return Collections.unmodifiableList(new ArrayList<>(allMessages));
        } finally {
            lock.unlock();
        }
    }

    /**
     * @return the configured maximum number of entries retained per list.
     */
    public int getMaxEntries() {
        return maxEntries;
    }

    /**
     * @return an unmodifiable list of all info messages handled by this instance.
     */
    public List<String> getAllInfoMessages() {
        lock.lock();
        try {
            return Collections.unmodifiableList(new ArrayList<>(allInfoMessages));
        } finally {
            lock.unlock();
        }
    }

    /**
     * @return an unmodifiable list of all warning messages handled by this instance.
     */
    public List<String> getAllWarnMessages() {
        lock.lock();
        try {
            return Collections.unmodifiableList(new ArrayList<>(allWarnMessages));
        } finally {
            lock.unlock();
        }
    }

    /**
     * @return an unmodifiable list of all error messages handled by this instance.
     */
    public List<String> getAllErrorMessages() {
        lock.lock();
        try {
            return Collections.unmodifiableList(new ArrayList<>(allErrorMessages));
        } finally {
            lock.unlock();
        }
    }

    /**
     * @return an unmodifiable list of all debug messages handled by this instance.
     */
    public List<String> getAllDebugMessages() {
        lock.lock();
        try {
            return Collections.unmodifiableList(new ArrayList<>(allDebugMessages));
        } finally {
            lock.unlock();
        }
    }

    /**
     * @return an unmodifiable list of all trace messages handled by this instance.
     */
    public List<String> getAllTraceMessages() {
        lock.lock();
        try {
            return Collections.unmodifiableList(new ArrayList<>(allTraceMessages));
        } finally {
            lock.unlock();
        }
    }

    /**
     * @return an unmodifiable list of all exception messages handled by this instance.
     */
    public List<String> getAllExceptionMessages() {
        lock.lock();
        try {
            return Collections.unmodifiableList(new ArrayList<>(allExceptionMessages));
        } finally {
            lock.unlock();
        }
    }

    /**
     * @return an unmodifiable list of all exceptions handled by this instance.
     */
    public List<Exception> getAllExceptions() {
        lock.lock();
        try {
            return Collections.unmodifiableList(new ArrayList<>(allExceptions));
        } finally {
            lock.unlock();
        }
    }

    /**
     * @return the last message handled by this instance.
     */
    public String getLastMessage() {
        lock.lock();
        try {
            return lastMessage;
        } finally {
            lock.unlock();
        }
    }
}
