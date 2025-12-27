package com.threeamigos.common.util.implementations.preferences.flavours;

import com.threeamigos.common.util.implementations.BasicPropertyChangeAware;
import com.threeamigos.common.util.interfaces.preferences.flavours.HintsPreferences;
import org.jspecify.annotations.NonNull;

import java.util.ResourceBundle;

public class HintsPreferencesImpl extends BasicPropertyChangeAware implements HintsPreferences {


    private static ResourceBundle bundle;

    private static ResourceBundle getBundle() {
        if (bundle == null) {
            bundle = ResourceBundle.getBundle("com.threeamigos.common.util.implementations.preferences.flavours.HintsPreferencesImpl.HintsPreferencesImpl");
        }
        return bundle;
    }

    // End of static methods

    private boolean hintsVisibleAtStartup;
    private int lastHintIndex = -1;
    private final String propertyName;

    public HintsPreferencesImpl(@NonNull String propertyName) {
        if (propertyName == null) {
            throw new IllegalArgumentException(getBundle().getString("noPropertyNameProvided"));
        }
        this.propertyName = propertyName;
    }

    @Override
    public @NonNull String getDescription() {
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

}
