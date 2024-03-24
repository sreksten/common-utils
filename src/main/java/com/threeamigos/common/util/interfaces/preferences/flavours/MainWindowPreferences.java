package com.threeamigos.common.util.interfaces.preferences.flavours;

/**
 * An interface that keeps track of the preferences for the main window of an application.
 *
 * @author Stefano Reksten
 */
public interface MainWindowPreferences extends WindowPreferences {

	public static final int MIN_WIDTH = 800;
	public static final int MIN_HEIGHT = 600;

	default String getDescription() {
		return "Main window preferences";
	}

}
