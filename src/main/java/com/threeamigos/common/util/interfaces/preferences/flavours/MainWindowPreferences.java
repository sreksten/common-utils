package com.threeamigos.common.util.interfaces.preferences.flavours;

import org.jspecify.annotations.NonNull;

/**
 * An interface that keeps track of the preferences for the main window of an application.
 *
 * @author Stefano Reksten
 */
public interface MainWindowPreferences extends WindowPreferences {

    int MIN_WIDTH = 800;
    int MIN_HEIGHT = 600;

    default @NonNull String getDescription() {
        return "Main window preferences";
    }

}
