package com.threeamigos.common.util.implementations.injection.methods;

import javax.inject.Inject;

public class ClassWithMethodWithInvalidParameter {

    private int field;

    @Inject
    private void methodWithInvalidParameter(int field) {
        this.field = field;
    }

    public int getField() {
        return field;
    }
}
