package com.threeamigos.common.util.interfaces.preferences.flavours;

/**
 * An interface that keeps track of the preferences for a secondary window of an application.
 *
 * @author Stefano Reksten
 */
public interface SecondaryWindowPreferences extends WindowPreferences {

    boolean VISIBLE_DEFAULT = false;

    /**
     * @param visible true if the window should be visible, thus opened at startup
     */
    void setVisible(boolean visible);

    /**
     * @return true if the window is currently visible
     */
    boolean isVisible();

}
