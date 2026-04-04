package com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticObserver.syntheticobservertest.test;

public class MyEvent {

    private final String payload;

    public MyEvent(String payload) {
        this.payload = payload;
    }

    public String getPayload() {
        return payload;
    }
}
