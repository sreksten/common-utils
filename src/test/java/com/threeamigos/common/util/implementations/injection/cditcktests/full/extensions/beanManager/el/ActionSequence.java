package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.beanManager.el;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class ActionSequence {

    private static final Map<String, List<String>> SEQUENCES = new HashMap<String, List<String>>();

    private ActionSequence() {
    }

    static synchronized void reset() {
        SEQUENCES.clear();
    }

    static synchronized void addAction(String sequenceName, String action) {
        List<String> data = SEQUENCES.get(sequenceName);
        if (data == null) {
            data = new ArrayList<String>();
            SEQUENCES.put(sequenceName, data);
        }
        data.add(action);
    }

    static synchronized List<String> getSequenceData(String sequenceName) {
        List<String> data = SEQUENCES.get(sequenceName);
        if (data == null) {
            return new ArrayList<String>();
        }
        return new ArrayList<String>(data);
    }
}
