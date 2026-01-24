package com.threeamigos.common.util.ui;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import javax.swing.*;
import javax.swing.SwingUtilities;
import java.lang.reflect.InvocationTargetException;

/**
 * A class making direct calls to the AWT. Used to decouple other classes and be able to test them
 * in a non-interactive way.
 *
 * @author Stefano Reksten
 */
public class AWTCalls {

    private AWTCalls() {
    }

    /**
     * Shows a JOptionPane.
     * @param parentComponent the parent window. Can be null.
     * @param message a message to show.
     * @param title a title for the message window.
     * @param icon should be one of the JOptionPane constants.
     */
    public static void showOptionPane(final @Nullable Component parentComponent, final @Nonnull String message,
                                      final @Nonnull String title, final int icon) {
        if (SwingUtilities.isEventDispatchThread()) {
            JOptionPane.showMessageDialog(parentComponent, message, title, icon);
        } else {
            try {
                SwingUtilities.invokeAndWait(() -> JOptionPane.showMessageDialog(parentComponent, message, title, icon));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e.getCause());
            }
        }
    }
}
