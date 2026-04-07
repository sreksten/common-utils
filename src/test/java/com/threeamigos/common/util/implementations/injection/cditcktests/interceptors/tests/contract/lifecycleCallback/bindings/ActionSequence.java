package com.threeamigos.common.util.implementations.injection.cditcktests.interceptors.tests.contract.lifecycleCallback.bindings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ActionSequence {

    private static final Map<String, ActionSequence> SEQUENCES = new HashMap<String, ActionSequence>();

    private final List<String> data = new ArrayList<String>();

    private ActionSequence() {
    }

    static synchronized void reset() {
        SEQUENCES.clear();
    }

    static synchronized boolean addAction(String sequenceName, String action) {
        ActionSequence sequence = SEQUENCES.get(sequenceName);
        boolean created = false;
        if (sequence == null) {
            sequence = new ActionSequence();
            SEQUENCES.put(sequenceName, sequence);
            created = true;
        }
        sequence.data.add(action);
        return created;
    }

    static synchronized ActionSequence getSequence(String sequenceName) {
        return SEQUENCES.get(sequenceName);
    }

    static synchronized List<String> getSequenceData(String sequenceName) {
        ActionSequence sequence = SEQUENCES.get(sequenceName);
        if (sequence == null) {
            return Collections.emptyList();
        }
        return new ArrayList<String>(sequence.data);
    }

    static synchronized int getSequenceSize(String sequenceName) {
        return getSequenceData(sequenceName).size();
    }

    void assertDataEquals(Class<?>... expected) {
        List<String> expectedNames = new ArrayList<String>();
        for (Class<?> clazz : Arrays.asList(expected)) {
            expectedNames.add(clazz.getSimpleName());
        }
        assertEquals(expectedNames, new ArrayList<String>(data));
    }
}
