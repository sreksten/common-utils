package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.beanDiscovery.event.ordering;

import org.junit.jupiter.api.Assertions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    synchronized List<String> getData() {
        return Collections.unmodifiableList(new ArrayList<String>(data));
    }

    void assertDataEquals(String... expected) {
        Assertions.assertEquals(Arrays.asList(expected), getData());
    }
}
