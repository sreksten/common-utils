package com.threeamigos.common.util.implementations.ui;

import com.threeamigos.common.util.interfaces.preferences.flavours.HintsPreferences;
import com.threeamigos.common.util.interfaces.ui.Hint;
import com.threeamigos.common.util.interfaces.ui.HintsCollector;

import java.util.ArrayList;
import java.util.List;

public class HintsSupport {

    private final HintsPreferences hintsPreferences;
    private final List<Hint<String>> hints;
    private int hintIndex;

    public HintsSupport(HintsPreferences hintsPreferences,
                        HintsCollector<String> hintsCollector) {
        this.hintsPreferences = hintsPreferences;
        hints = new ArrayList<>();
        hints.addAll(hintsCollector.getHints());
        hintIndex = hintsPreferences.getLastHintIndex();
        if (hints.isEmpty()) {
            hints.add(new StringHint("No hints were provided."));
        }
        if (hintIndex > hints.size()) {
            hintIndex = 0;
        }
    }

    private Hint<String> getHintImpl(int offset) {
        hintIndex = (hintsPreferences.getLastHintIndex() + offset);
        if (hintIndex >= hints.size()) {
            hintIndex = 0;
        } else if (hintIndex < 0) {
            hintIndex = hints.size() - 1;
        }
        hintsPreferences.setLastHintIndex(hintIndex);
        return hints.get(hintIndex);
    }

    public boolean isHintsVisibleAtStartup() {
        return hintsPreferences.isHintsVisibleAtStartup();
    }

    public void setHintsVisibleAtStartup(boolean visibleAtStartup) {
        hintsPreferences.setHintsVisibleAtStartup(visibleAtStartup);
    }

    public int getCurrentHintIndex() {
        return hintIndex;
    }

    public int getTotalHints() {
        return hints.size();
    }

    public Hint<String> getNextHint() {
        return getHintImpl(1);
    }

    public Hint<String> getPreviousHint() {
        return getHintImpl(-1);
    }

}
