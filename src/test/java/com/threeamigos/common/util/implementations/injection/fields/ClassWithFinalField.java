package com.threeamigos.common.util.implementations.injection.fields;

import javax.inject.Inject;

public class ClassWithFinalField {

    @Inject
    private final int finalField = 1;

    public int getFinalField() {
        return finalField;
    }

}
