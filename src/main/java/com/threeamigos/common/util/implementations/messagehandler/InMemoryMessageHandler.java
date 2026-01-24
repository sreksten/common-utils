package com.threeamigos.common.util.implementations.messagehandler;

import com.threeamigos.common.util.interfaces.messagehandler.MessageHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An implementation of the {@link MessageHandler} interface that stores
 * messages and exceptions to an internal list for later processing (e.g.,
 * to conduct unit testing).
 *
 * @author Stefano Reksten
 */
public class InMemoryMessageHandler extends AbstractMessageHandler {

    private final List<String> allMessages = new ArrayList<>();
    private final List<String> allInfoMessages = new ArrayList<>();
    private final List<String> allWarnMessages = new ArrayList<>();
    private final List<String> allErrorMessages = new ArrayList<>();
    private final List<String> allDebugMessages = new ArrayList<>();
    private final List<String> allTraceMessages = new ArrayList<>();
    private final List<String> allExceptionMessages = new ArrayList<>();
    private final List<Exception> allExceptions = new ArrayList<>();
    private String lastMessage;

    @Override
    protected void handleInfoMessageImpl(final String message) {
        allInfoMessages.add(message);
        handleImpl(message);
    }

    @Override
    protected void handleWarnMessageImpl(final String message) {
        allWarnMessages.add(message);
        handleImpl(message);
    }

    @Override
    protected void handleErrorMessageImpl(final String message) {
        allErrorMessages.add(message);
        handleImpl(message);
    }

    @Override
    protected void handleDebugMessageImpl(final String message) {
        allDebugMessages.add(message);
        handleImpl(message);
    }

    @Override
    protected void handleTraceMessageImpl(final String message) {
        allTraceMessages.add(message);
        handleImpl(message);
    }

    @Override
    protected void handleExceptionImpl(final Exception exception) {
        allExceptionMessages.add(exception.getMessage());
        allExceptions.add(exception);
        handleImpl(exception.getMessage());
    }

    private void handleImpl(final String message) {
        allMessages.add(message);
        lastMessage = message;
    }

    /**
     * @return an unmodifiable list of all messages handled by this instance.
     */
    public List<String> getAllMessages() {
        return Collections.unmodifiableList(allMessages);
    }

    /**
     * @return an unmodifiable list of all info messages handled by this instance.
     */
    public List<String> getAllInfoMessages() {
        return Collections.unmodifiableList(allInfoMessages);
    }

    /**
     * @return an unmodifiable list of all warning messages handled by this instance.
     */
    public List<String> getAllWarnMessages() {
        return Collections.unmodifiableList(allWarnMessages);
    }

    /**
     * @return an unmodifiable list of all error messages handled by this instance.
     */
    public List<String> getAllErrorMessages() {
        return Collections.unmodifiableList(allErrorMessages);
    }

    /**
     * @return an unmodifiable list of all debug messages handled by this instance.
     */
    public List<String> getAllDebugMessages() {
        return Collections.unmodifiableList(allDebugMessages);
    }

    /**
     * @return an unmodifiable list of all trace messages handled by this instance.
     */
    public List<String> getAllTraceMessages() {
        return Collections.unmodifiableList(allTraceMessages);
    }

    /**
     * @return an unmodifiable list of all exception messages handled by this instance.
     */
    public List<String> getAllExceptionMessages() {
        return Collections.unmodifiableList(allExceptionMessages);
    }

    /**
     * @return an unmodifiable list of all exceptions handled by this instance.
     */
    public List<Exception> getAllExceptions() {
        return Collections.unmodifiableList(allExceptions);
    }

    /**
     * @return the last message handled by this instance.
     */
    public String getLastMessage() {
        return lastMessage;
    }
}
