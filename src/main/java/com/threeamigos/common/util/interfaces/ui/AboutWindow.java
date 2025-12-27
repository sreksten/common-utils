package com.threeamigos.common.util.interfaces.ui;

import org.jspecify.annotations.Nullable;

import java.awt.*;

/**
 * Shows information about this application.
 *
 * @author Stefano Reksten
 */
@FunctionalInterface
public interface AboutWindow {

    /**
     * Shows information about this application.
     * @param component the parent component containing the window (can be null)
     */
    void about(@Nullable Component component);

}
