package com.threeamigos.common.util.implementations.injection.fields;

import jakarta.inject.Inject;

public class ClassWithFinalField {

    @Inject
    @SuppressWarnings("all")
    private final int finalField = 1;

}
