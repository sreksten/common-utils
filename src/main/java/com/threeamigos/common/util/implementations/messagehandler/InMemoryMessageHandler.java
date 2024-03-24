package com.threeamigos.common.util.implementations.messagehandler;

import com.threeamigos.common.util.interfaces.messagehandler.MessageHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An implementation of the {@link MessageHandler} interface that stores
 * messages and exceptions to an internal list for subsequent processing (e.g.
 * to conduct unit testing).
 *
 * @author Stefano Reksten
 */
public class InMemoryMessageHandler implements MessageHandler {

    private final List<String> allMessages = new ArrayList<>();
    private final List<String> allInfoMessages = new ArrayList<>();
    private final List<String> allWarnMessages = new ArrayList<>();
    private final List<String> allErrorMessages = new ArrayList<>();
    private final List<String> allExceptionMessages = new ArrayList<>();
    private final List<Exception> allExceptions = new ArrayList<>();
    private String lastMessage;

    @Override
    public void handleInfoMessage(final String message) {
        handleImpl(message);
        allInfoMessages.add(message);
    }

    @Override
    public void handleWarnMessage(final String message) {
        handleImpl(message);
        allWarnMessages.add(message);
    }

    @Override
    public void handleErrorMessage(final String message) {
        handleImpl(message);
        allErrorMessages.add(message);
    }

    @Override
    public void handleException(final Exception exception) {
        handleImpl(exception.getMessage());
        allExceptionMessages.add(exception.getMessage());
        allExceptions.add(exception);
    }

    private void handleImpl(final String message) {
        allMessages.add(message);
        lastMessage = message;
    }

    public List<String> getAllMessages() {
        return Collections.unmodifiableList(allMessages);
    }

    public List<String> getAllInfoMessages() {
        return Collections.unmodifiableList(allInfoMessages);
    }

    public List<String> getAllWarnMessages() {
        return Collections.unmodifiableList(allWarnMessages);
    }

    public List<String> getAllErrorMessages() {
        return Collections.unmodifiableList(allErrorMessages);
    }

    public List<String> getAllExceptionMessages() {
        return Collections.unmodifiableList(allExceptionMessages);
    }

    public List<Exception> getAllExceptions() {
        return Collections.unmodifiableList(allExceptions);
    }

    public String getLastMessage() {
        return lastMessage;
    }

}
