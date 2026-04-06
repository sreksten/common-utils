package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.disposal.method.definition.broken.multiple;

public class Bus implements Vehicle {

    @SuppressWarnings("unused")
    private final String name;

    public Bus(String name) {
        this.name = name;
    }
}
