package com.threeamigos.common.util.implementations.messagehandler;

import com.threeamigos.common.util.interfaces.messagehandler.MessageHandler;
import com.threeamigos.common.util.ui.AWTCalls;
import org.jspecify.annotations.NonNull;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

/**
 * An implementation of the {@link MessageHandler} interface that uses an
 * OptionPane to show messages and exceptions.
 *
 * @author Stefano Reksten
 */
public class SwingMessageHandler implements MessageHandler {

    private static ResourceBundle bundle;

    private static ResourceBundle getBundle() {
        if (bundle == null) {
            bundle = ResourceBundle.getBundle("com.threeamigos.common.util.implementations.messagehandler.SwingMessageHandler.SwingMessageHandler");
        }
        return bundle;
    }

    private Component parentComponent;

    public SwingMessageHandler(final Component parentComponent) {
        this.parentComponent = parentComponent;
    }

    public SwingMessageHandler() {
    }

    public void setParentComponent(final Component parentComponent) {
        this.parentComponent = parentComponent;
    }

    public Component getParentComponent() {
        return parentComponent;
    }

    public void handleInfoMessage(final @NonNull String message) {
        if (message == null) {
            throw new IllegalArgumentException(getBundle().getString("nullMessageProvided"));
        }
        AWTCalls.showOptionPane(parentComponent, message, getBundle().getString("info"), JOptionPane.INFORMATION_MESSAGE);
    }

    public void handleWarnMessage(final @NonNull String message) {
        if (message == null) {
            throw new IllegalArgumentException(getBundle().getString("nullMessageProvided"));
        }
        AWTCalls.showOptionPane(parentComponent, message, getBundle().getString("warning"), JOptionPane.WARNING_MESSAGE);
    }

    public void handleErrorMessage(final @NonNull String message) {
        if (message == null) {
            throw new IllegalArgumentException(getBundle().getString("nullMessageProvided"));
        }
        AWTCalls.showOptionPane(parentComponent, message, getBundle().getString("error"), JOptionPane.ERROR_MESSAGE);
    }

    public void handleDebugMessage(final @NonNull String message) {
        if (message == null) {
            throw new IllegalArgumentException(getBundle().getString("nullMessageProvided"));
        }
        AWTCalls.showOptionPane(parentComponent, message, getBundle().getString("debug"), JOptionPane.INFORMATION_MESSAGE);
    }

    public void handleTraceMessage(final @NonNull String message) {
        if (message == null) {
            throw new IllegalArgumentException(getBundle().getString("nullMessageProvided"));
        }
        AWTCalls.showOptionPane(parentComponent, message, getBundle().getString("trace"), JOptionPane.INFORMATION_MESSAGE);
    }

    public void handleException(final @NonNull Exception exception) {
        if (exception == null) {
            throw new IllegalArgumentException(getBundle().getString("nullExceptionProvided"));
        }
        AWTCalls.showOptionPane(parentComponent, exception.getMessage(), getBundle().getString("exception"), JOptionPane.ERROR_MESSAGE);
    }
}
