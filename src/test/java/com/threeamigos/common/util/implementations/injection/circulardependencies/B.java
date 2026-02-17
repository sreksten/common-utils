package com.threeamigos.common.util.implementations.injection.circulardependencies;

import jakarta.inject.Inject;

public class B {

    @Inject
    A a;
}
