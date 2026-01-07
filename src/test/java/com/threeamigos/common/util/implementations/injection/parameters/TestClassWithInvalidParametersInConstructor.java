package com.threeamigos.common.util.implementations.injection.parameters;

import javax.inject.Inject;

public class TestClassWithInvalidParametersInConstructor {

    @SuppressWarnings("all")
    private final int count;

    @Inject
    @SuppressWarnings("all")
    public TestClassWithInvalidParametersInConstructor(int count) {
        this.count = count;
    }
}
