package com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticObserver.syntheticobservertest.test;

import java.util.ArrayList;
import java.util.List;

final class ObservedMessages {

    private static final List<String> OBSERVED = new ArrayList<String>();

    private ObservedMessages() {
    }

    static void add(String message) {
        OBSERVED.add(message);
    }

    static void reset() {
        OBSERVED.clear();
    }

    static List<String> getObserved() {
        return new ArrayList<String>(OBSERVED);
    }
}
