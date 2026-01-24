package com.threeamigos.common.util.implementations.ui;

import com.threeamigos.common.util.interfaces.ui.AboutWindow;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

/**
 * An implementation of a window showing info about this application
 *
 * @author Stefano Reksten
 */
public class AboutWindowImpl implements AboutWindow {

    private final String applicationName;
    private final URL imgUrl;
    private final String[] releaseNotes;

    public AboutWindowImpl(String applicationName, String... releaseNotes) {
        this.applicationName = applicationName;
        this.imgUrl = getClass().getResource("/3AM_logo.png");
        this.releaseNotes = releaseNotes;
    }

    public AboutWindowImpl(String applicationName, URL imageUrl, String... releaseNotes) {
        this.applicationName = applicationName;
        this.imgUrl = imageUrl;
        this.releaseNotes = releaseNotes;
    }

    @Override
    public void about(@Nullable Component component) {

        Box panel = Box.createVerticalBox();

        JLabel logo = new JLabel(new ImageIcon(imgUrl));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(logo);

        panel.add(Box.createVerticalStrut(10));

        JLabel applicationLabel = new JLabel(applicationName);
        Font font = new Font("Serif", Font.BOLD, 16);
        applicationLabel.setFont(font);
        applicationLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(applicationLabel);

        for (String note : releaseNotes) {
            panel.add(Box.createVerticalStrut(5));

            JLabel label = new JLabel(note);
            label.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(label);
        }

        JOptionPane.showOptionDialog(component, panel, applicationName, JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE, null, null, null);
    }
}
