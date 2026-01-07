package com.threeamigos.common.util.implementations.injection.fields;

import javax.inject.Inject;

public class ClassWithStaticField {

    @Inject
    @SuppressWarnings("all")
    private static int staticField = 1;

}
