package com.threeamigos.common.util.implementations.injection.methods;

import javax.inject.Inject;

public class ClassWithMethodWithInvalidParameter {

    @SuppressWarnings("all")
    private int field;

    @Inject
    private void methodWithInvalidParameter(int field) {
        this.field = field;
    }

}
