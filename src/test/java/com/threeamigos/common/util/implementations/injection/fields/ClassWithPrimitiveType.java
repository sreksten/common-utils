package com.threeamigos.common.util.implementations.injection.fields;

import javax.inject.Inject;

public class ClassWithPrimitiveType {

    @Inject
    private int primitiveField;

    public int getPrimitiveField() {
        return primitiveField;
    }
}
