package com.threeamigos.common.util.implementations.injection.cditcktests.context.dependent.transientreference.dependenttransientreferencedestroyedtest.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ActionSequence {

    private static final List<String> DATA = Collections.synchronizedList(new ArrayList<String>());

    private ActionSequence() {
    }

    public static void reset() {
        DATA.clear();
    }

    public static void addAction(String actionId) {
        DATA.add(actionId);
    }

    public static int getSequenceSize() {
        return DATA.size();
    }

    public static ActionSequence getSequence() {
        return new ActionSequence();
    }

    public void assertDataContainsAll(String... expected) {
        for (String item : Arrays.asList(expected)) {
            assertTrue(DATA.contains(item), "Action sequence does not contain: " + item + ", data=" + DATA);
        }
    }
}
