package com.threeamigos.common.util.ui;

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

    public static void showOptionPane(Component parentComponent, String message, String title, int icon) {
        JOptionPane.showMessageDialog(parentComponent, message, title, icon);
    }
}
