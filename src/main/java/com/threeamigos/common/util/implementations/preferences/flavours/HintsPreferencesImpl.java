package com.threeamigos.common.util.implementations.preferences.flavours;

import com.threeamigos.common.util.implementations.BasicPropertyChangeAware;
import com.threeamigos.common.util.interfaces.preferences.flavours.HintsPreferences;

public class HintsPreferencesImpl extends BasicPropertyChangeAware implements HintsPreferences {

    private boolean hintsVisibleAtStartup;
    private int lastHintIndex = -1;
    private final String propertyName;

    public HintsPreferencesImpl(String propertyName) {
        this.propertyName = propertyName;
    }

    @Override
    public String getDescription() {
        return "hints preferences";
    }

    @Override
    public void setHintsVisibleAtStartup(boolean hintsVisibleAtStartup) {
        boolean oldHintsVisibleAtStartup = this.hintsVisibleAtStartup;
        this.hintsVisibleAtStartup = hintsVisibleAtStartup;
        firePropertyChange(propertyName, oldHintsVisibleAtStartup,
                hintsVisibleAtStartup);
    }

    @Override
    public boolean isHintsVisibleAtStartup() {
        return hintsVisibleAtStartup;
    }

    @Override
    public void setLastHintIndex(int lastHintIndex) {
        int oldLastHintIndex = this.lastHintIndex;
        this.lastHintIndex = lastHintIndex;
        firePropertyChange(propertyName, oldLastHintIndex, lastHintIndex);
    }

    @Override
    public int getLastHintIndex() {
        return lastHintIndex;
    }

    @Override
    public void loadDefaultValues() {
        hintsVisibleAtStartup = HINTS_PREFERENCES_VISIBLE_DEFAULT;
        lastHintIndex = HINTS_PREFERENCES_INDEX_DEFAULT;
    }

    @Override
    public void validate() {
    }

}
