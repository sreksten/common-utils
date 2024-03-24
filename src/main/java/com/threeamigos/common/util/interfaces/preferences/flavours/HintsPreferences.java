package com.threeamigos.common.util.interfaces.preferences.flavours;

import com.threeamigos.common.util.interfaces.preferences.Preferences;

/**
 * A set of preferences that manages startup hints for an application.
 *
 * @author Stefano Reksten
 */
public interface HintsPreferences extends Preferences {

	public static final boolean HINTS_PREFERENCES_VISIBLE_DEFAULT = true;
	public static final int HINTS_PREFERENCES_INDEX_DEFAULT = -1;

	/**
	 * @param hintsVisibleAtStartup true if hints should be shown at startup.
	 */
	public void setHintsVisibleAtStartup(boolean hintsVisibleAtStartup);

	/**
	 * @return true if hints should be visible at startup.
	 */
	public boolean isHintsVisibleAtStartup();

	/**
	 * @param lastHintIndex index of the last hint shown to the user.
	 */
	public void setLastHintIndex(int lastHintIndex);

	/**
	 * @return index of last hint shown to the user.
	 */
	public int getLastHintIndex();

}
