package com.threeamigos.common.util.implementations.messagehandler;

import com.threeamigos.common.util.interfaces.messagehandler.MessageHandler;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

/**
 * An implementation of the {@link MessageHandler} interface that stores
 * messages and exceptions to an internal list for later processing (e.g.,
 * to conduct unit testing).
 *
 * @author Stefano Reksten
 */
public class InMemoryMessageHandler implements MessageHandler {

    private static ResourceBundle bundle;

    private static ResourceBundle getBundle() {
        if (bundle == null) {
            bundle = ResourceBundle.getBundle("com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler.InMemoryMessageHandler");
        }
        return bundle;
    }

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
    public void handleInfoMessage(final @NonNull String message) {
        handleImpl(message);
        allInfoMessages.add(message);
    }

    @Override
    public void handleWarnMessage(final @NonNull String message) {
        handleImpl(message);
        allWarnMessages.add(message);
    }

    @Override
    public void handleErrorMessage(final @NonNull String message) {
        handleImpl(message);
        allErrorMessages.add(message);
    }

    @Override
    public void handleDebugMessage(final @NonNull String message) {
        handleImpl(message);
        allDebugMessages.add(message);
    }

    @Override
    public void handleTraceMessage(final @NonNull String message) {
        handleImpl(message);
        allTraceMessages.add(message);
    }

    @Override
    public void handleException(final @NonNull Exception exception) {
        if (exception == null) {
            throw new IllegalArgumentException(getBundle().getString("nullExceptionProvided"));
        }
        handleImpl(exception.getMessage());
        allExceptionMessages.add(exception.getMessage());
        allExceptions.add(exception);
    }

    private void handleImpl(final String message) {
        if (message == null) {
            throw new IllegalArgumentException(getBundle().getString("nullMessageProvided"));
        }
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

    public List<String> getAllDebugMessages() {
        return Collections.unmodifiableList(allDebugMessages);
    }

    public List<String> getAllTraceMessages() {
        return Collections.unmodifiableList(allTraceMessages);
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
