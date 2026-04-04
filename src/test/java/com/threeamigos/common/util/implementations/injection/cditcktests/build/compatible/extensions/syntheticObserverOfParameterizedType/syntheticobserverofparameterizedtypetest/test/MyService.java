package com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticObserverOfParameterizedType.syntheticobserverofparameterizedtypetest.test;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class MyService {

    @Inject
    Event<Object> event;

    public void fireEvent() {
        Event<List> selected = event.select(List.class);
        selected.fire(new MyDataList(new MyData("Hello"), new MyData("World")));
        selected.fire(new MyDataList(new MyData("Hello"), new MyData("again")));
    }
}
