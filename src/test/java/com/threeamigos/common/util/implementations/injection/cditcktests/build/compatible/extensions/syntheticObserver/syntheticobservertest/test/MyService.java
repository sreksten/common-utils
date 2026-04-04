package com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticObserver.syntheticobservertest.test;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

@ApplicationScoped
public class MyService {

    @Inject
    Event<MyEvent> unqualifiedEvent;

    @Inject
    @MyQualifier
    Event<MyEvent> qualifiedEvent;

    public void fireEvent() {
        unqualifiedEvent.fire(new MyEvent("Hello World"));
        qualifiedEvent.fire(new MyEvent("Hello Special"));
    }
}
