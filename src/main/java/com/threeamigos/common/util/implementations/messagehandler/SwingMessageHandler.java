package com.threeamigos.common.util.implementations.messagehandler;

import com.threeamigos.common.util.interfaces.messagehandler.MessageHandler;
import com.threeamigos.common.util.ui.AWTCalls;

import javax.swing.*;
import java.awt.*;

/**
 * An implementation of the {@link MessageHandler} interface that uses an
 * OptionPane to show messages and exceptions.
 *
 * @author Stefano Reksten
 */
public class SwingMessageHandler implements MessageHandler {

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

    public void handleInfoMessage(final String message) {
        AWTCalls.showOptionPane(parentComponent, message, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    public void handleWarnMessage(final String message) {
        AWTCalls.showOptionPane(parentComponent, message, "Warning", JOptionPane.WARNING_MESSAGE);
    }

    public void handleErrorMessage(final String message) {
        AWTCalls.showOptionPane(parentComponent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public void handleException(final Exception exception) {
        AWTCalls.showOptionPane(parentComponent, exception.getMessage(), "Exception", JOptionPane.ERROR_MESSAGE);
    }
}
