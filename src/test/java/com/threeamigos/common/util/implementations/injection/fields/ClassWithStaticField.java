package com.threeamigos.common.util.implementations.injection.fields;

import javax.inject.Inject;

public class ClassWithStaticField {

    @Inject
    private static int staticField = 1;

    public static int getStaticField() {
        return staticField;
    }

}
