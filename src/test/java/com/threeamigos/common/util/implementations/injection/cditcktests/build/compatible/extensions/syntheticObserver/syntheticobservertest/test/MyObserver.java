package com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticObserver.syntheticobservertest.test;

import jakarta.enterprise.inject.build.compatible.spi.Parameters;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticObserver;
import jakarta.enterprise.inject.spi.EventContext;

import java.util.List;

public class MyObserver implements SyntheticObserver<MyEvent> {

    @Override
    public void observe(EventContext<MyEvent> event, Parameters params) {
        String name = params.get("name", String.class);
        ObservedMessages.add(event.getEvent().getPayload() + " with " + name);
    }

    public static void reset() {
        ObservedMessages.reset();
    }

    public static List<String> getObserved() {
        return ObservedMessages.getObserved();
    }
}
