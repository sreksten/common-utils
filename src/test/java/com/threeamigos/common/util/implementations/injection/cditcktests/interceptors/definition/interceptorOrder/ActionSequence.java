package com.threeamigos.common.util.implementations.injection.cditcktests.interceptors.definition.interceptorOrder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class ActionSequence {

    private static final String DEFAULT_KEY = "";
    private static final Map<String, List<String>> DATA = new HashMap<String, List<String>>();

    private ActionSequence() {
    }

    static synchronized void addAction(String action) {
        addAction(DEFAULT_KEY, action);
    }

    static synchronized void addAction(String sequenceName, String action) {
        List<String> actions = DATA.get(sequenceName);
        if (actions == null) {
            actions = new ArrayList<String>();
            DATA.put(sequenceName, actions);
        }
        actions.add(action);
    }

    static synchronized void reset() {
        DATA.clear();
    }

    static synchronized List<String> getSequenceData() {
        return getSequenceData(DEFAULT_KEY);
    }

    static synchronized List<String> getSequenceData(String sequenceName) {
        List<String> actions = DATA.get(sequenceName);
        if (actions == null) {
            return Collections.emptyList();
        }
        return new ArrayList<String>(actions);
    }
}
