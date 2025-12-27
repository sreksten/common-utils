package com.threeamigos.common.util.interfaces.preferences.flavours;

import com.threeamigos.common.util.interfaces.preferences.Preferences;

/**
 * A set of preferences that manages startup hints for an application.
 *
 * @author Stefano Reksten
 */
public interface HintsPreferences extends Preferences {

    boolean HINTS_PREFERENCES_VISIBLE_DEFAULT = true;
    int HINTS_PREFERENCES_INDEX_DEFAULT = -1;

    /**
     * @param hintsVisibleAtStartup true if hints should be shown at startup.
     */
    void setHintsVisibleAtStartup(boolean hintsVisibleAtStartup);

    /**
     * @return true if hints should be visible at startup.
     */
    boolean isHintsVisibleAtStartup();

    /**
     * @param lastHintIndex index of the last hint shown to the user.
     */
    void setLastHintIndex(int lastHintIndex);

    /**
     * @return index of the last hint shown to the user.
     */
    int getLastHintIndex();

}
