package com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticObserver.syntheticobservertest.test;

import jakarta.enterprise.inject.build.compatible.spi.Parameters;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticObserver;
import jakarta.enterprise.inject.spi.EventContext;

public class MyQualifiedObserver implements SyntheticObserver<Object> {

    @Override
    public void observe(EventContext<Object> event, Parameters params) {
        String name = params.get("name", String.class, null);
        MyEvent myEvent = (MyEvent) event.getEvent();
        ObservedMessages.add(myEvent.getPayload() + " with " + name);
    }
}
