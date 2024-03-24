package com.threeamigos.common.util.implementations.ui;

import com.threeamigos.common.util.interfaces.ui.AboutWindow;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

/**
 * An implementation of an about window
 *
 * @author Stefano Reksten
 */
public class AboutWindowImpl implements AboutWindow {

    private final String applicationName;
    private final String author;
    private final String releaseNotes;
    private final URL imgUrl;

    public AboutWindowImpl(String applicationName, String author, String releaseNotes) {
        this.applicationName = applicationName;
        this.author = author;
        this.releaseNotes = releaseNotes;
        this.imgUrl = getClass().getResource("/3AM_logo.png");
    }

    public AboutWindowImpl(String applicationName, String author, String releaseNotes, URL imageUrl) {
        this.applicationName = applicationName;
        this.author = author;
        this.releaseNotes = releaseNotes;
        this.imgUrl = imageUrl;
    }

    @Override
    public void about(Component component) {

        Box panel = Box.createVerticalBox();

        JLabel logo = new JLabel(new ImageIcon(imgUrl));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(logo);

        panel.add(Box.createVerticalStrut(10));

        JLabel mandelbrotLabel = new JLabel(applicationName);
        Font font = new Font("Serif", Font.BOLD, 16);
        mandelbrotLabel.setFont(font);
        mandelbrotLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(mandelbrotLabel);

        panel.add(Box.createVerticalStrut(5));

        JLabel authorLabel = new JLabel(author);
        authorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(authorLabel);

        panel.add(Box.createVerticalStrut(5));

        JLabel license = new JLabel(releaseNotes);
        license.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(license);

        JOptionPane.showOptionDialog(component, panel, applicationName, JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE, null, null, null);
    }
}
