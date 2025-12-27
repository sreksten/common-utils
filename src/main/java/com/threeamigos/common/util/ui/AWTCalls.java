package com.threeamigos.common.util.ui;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

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
    public static void showOptionPane(@Nullable Component parentComponent, @NonNull String message, @NonNull String title, int icon) {
        JOptionPane.showMessageDialog(parentComponent, message, title, icon);
    }
}
