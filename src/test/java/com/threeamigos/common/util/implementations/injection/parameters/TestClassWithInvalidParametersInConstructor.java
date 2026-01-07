package com.threeamigos.common.util.implementations.injection.parameters;

import javax.inject.Inject;

public class TestClassWithInvalidParametersInConstructor {

    private final int count;

    @Inject
    public TestClassWithInvalidParametersInConstructor(int count) {
        this.count = count;
    }
}
