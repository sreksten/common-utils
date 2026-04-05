package com.threeamigos.common.util.implementations.injection.cditcktests.full.event.observer.priority;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class ActionSequence {

    private static final List<String> ACTIONS = new ArrayList<String>();

    private ActionSequence() {
    }

    static synchronized void addAction(String action) {
        ACTIONS.add(action);
    }

    static synchronized List<String> getActions() {
        return Collections.unmodifiableList(new ArrayList<String>(ACTIONS));
    }
}
