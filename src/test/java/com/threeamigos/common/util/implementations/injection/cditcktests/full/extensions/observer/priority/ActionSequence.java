package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.observer.priority;

import org.junit.jupiter.api.Assertions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class ActionSequence {

    private static final List<String> DATA = new ArrayList<String>();

    private ActionSequence() {
    }

    static synchronized void reset() {
        DATA.clear();
    }

    static synchronized void addAction(String action) {
        DATA.add(action);
    }

    static synchronized void assertSequenceDataEquals(String... expected) {
        Assertions.assertEquals(Arrays.asList(expected), new ArrayList<String>(DATA));
    }
}
