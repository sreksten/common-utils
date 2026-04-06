package com.threeamigos.common.util.implementations.injection.cditcktests.interceptors.tests.contract.aroundInvoke.bindings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    static synchronized void assertSequenceDataContainsAll(Class<?>... expected) {
        List<String> actual = new ArrayList<String>(DATA);
        for (Class<?> clazz : Arrays.asList(expected)) {
            assertTrue(actual.contains(clazz.getSimpleName()));
        }
    }

    static synchronized void assertSequenceDataEquals(Class<?>... expected) {
        List<String> expectedNames = new ArrayList<String>();
        for (Class<?> clazz : Arrays.asList(expected)) {
            expectedNames.add(clazz.getSimpleName());
        }
        assertEquals(expectedNames, new ArrayList<String>(DATA));
    }
}
