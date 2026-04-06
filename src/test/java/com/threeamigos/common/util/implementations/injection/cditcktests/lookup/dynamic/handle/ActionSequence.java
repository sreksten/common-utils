package com.threeamigos.common.util.implementations.injection.cditcktests.lookup.dynamic.handle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class ActionSequence {

    private static final List<String> SEQUENCE = Collections.synchronizedList(new ArrayList<String>());

    private ActionSequence() {
    }

    static void addAction(String action) {
        SEQUENCE.add(action);
    }

    static void reset() {
        SEQUENCE.clear();
    }

    static List<String> getSequenceData() {
        synchronized (SEQUENCE) {
            return new ArrayList<String>(SEQUENCE);
        }
    }

    static int getSequenceSize() {
        return SEQUENCE.size();
    }
}
