package com.threeamigos.common.util.implementations.messagehandler;

import com.threeamigos.common.util.interfaces.messagehandler.MessageHandler;
import com.threeamigos.common.util.ui.AWTCalls;
import org.jspecify.annotations.NonNull;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

/**
 * An implementation of the {@link MessageHandler} interface that uses an
 * OptionPane to show messages and exceptions to the user.
 *
 * @author Stefano Reksten
 */
public class SwingMessageHandler extends AbstractMessageHandler {

    private static ResourceBundle bundle;

    private static ResourceBundle getBundle() {
        if (bundle == null) {
            bundle = ResourceBundle.getBundle("com.threeamigos.common.util.implementations.messagehandler.SwingMessageHandler.SwingMessageHandler");
        }
        return bundle;
    }

    // End of static methods

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

    protected void handleInfoMessageImpl(final @NonNull String message) {
        AWTCalls.showOptionPane(parentComponent, message, getBundle().getString("info"), JOptionPane.INFORMATION_MESSAGE);
    }

    protected void handleWarnMessageImpl(final @NonNull String message) {
        AWTCalls.showOptionPane(parentComponent, message, getBundle().getString("warning"), JOptionPane.WARNING_MESSAGE);
    }

    protected void handleErrorMessageImpl(final @NonNull String message) {
        AWTCalls.showOptionPane(parentComponent, message, getBundle().getString("error"), JOptionPane.ERROR_MESSAGE);
    }

    protected void handleDebugMessageImpl(final @NonNull String message) {
        AWTCalls.showOptionPane(parentComponent, message, getBundle().getString("debug"), JOptionPane.INFORMATION_MESSAGE);
    }

    protected void handleTraceMessageImpl(final @NonNull String message) {
        AWTCalls.showOptionPane(parentComponent, message, getBundle().getString("trace"), JOptionPane.INFORMATION_MESSAGE);
    }

    protected void handleExceptionImpl(final @NonNull Exception exception) {
        AWTCalls.showOptionPane(parentComponent, exception.getMessage(), getBundle().getString("exception"), JOptionPane.ERROR_MESSAGE);
    }
}
