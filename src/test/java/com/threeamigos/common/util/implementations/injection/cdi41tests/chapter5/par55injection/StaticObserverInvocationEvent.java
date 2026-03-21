package com.threeamigos.common.util.implementations.injection.cdi41tests.chapter5.par55injection;

public class StaticObserverInvocationEvent {

    private final String id;

    public StaticObserverInvocationEvent(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
