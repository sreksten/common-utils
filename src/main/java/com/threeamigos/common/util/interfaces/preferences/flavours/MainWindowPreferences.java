package com.threeamigos.common.util.interfaces.preferences.flavours;

import jakarta.annotation.Nonnull;

/**
 * An interface that keeps track of the preferences for the main window of an application.
 *
 * @author Stefano Reksten
 */
public interface MainWindowPreferences extends WindowPreferences {

    int MIN_WIDTH = 800;
    int MIN_HEIGHT = 600;

    default @Nonnull String getDescription() {
        return "Main window preferences";
    }

}
