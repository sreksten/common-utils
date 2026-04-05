package com.threeamigos.common.util.implementations.injection.cditcktests.full.deployment.initialization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class ActionSequence {

    private static final List<String> SEQUENCE = Collections.synchronizedList(new ArrayList<String>());

    private ActionSequence() {
    }

    static void reset() {
        synchronized (SEQUENCE) {
            SEQUENCE.clear();
        }
    }

    static void addAction(String actionId) {
        SEQUENCE.add(actionId);
    }

    static List<String> getSequenceData() {
        synchronized (SEQUENCE) {
            return new ArrayList<String>(SEQUENCE);
        }
    }
}
