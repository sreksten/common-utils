package com.threeamigos.common.util.implementations.injection.circulardependencies;

import javax.inject.Inject;

public class B {

    @Inject
    A a;
}
