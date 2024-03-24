package com.threeamigos.common.util.interfaces.preferences.flavours;

/**
 * An interface that keeps track of preferences of a secondary window of an application.
 *
 * @author Stefano Reksten
 */
public interface SecondaryWindowPreferences extends WindowPreferences {

	public static final boolean VISIBLE_DEFAULT = false;

	/**
	 * @param visible true if the window should be visible, thus opened at startup
	 */
	public void setVisible(boolean visible);

	/**
	 * @return true if the window is currently visible
	 */
	public boolean isVisible();

}
