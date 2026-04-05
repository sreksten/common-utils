package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.interceptionFactory;

import org.junit.jupiter.api.Assertions;

import java.util.ArrayList;
import java.util.List;

final class ActionSequence {

    private static final List<String> ACTIONS = new ArrayList<String>();

    private ActionSequence() {
    }

    static synchronized void reset() {
        ACTIONS.clear();
    }

    static synchronized void addAction(String action) {
        ACTIONS.add(action);
    }

    static synchronized void assertSequenceDataEquals(Class<?>... expectedClasses) {
        List<String> expected = new ArrayList<String>();
        for (Class<?> expectedClass : expectedClasses) {
            expected.add(expectedClass.getSimpleName());
        }
        Assertions.assertEquals(expected, new ArrayList<String>(ACTIONS));
    }
}
